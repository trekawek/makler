package pl.net.newton.Makler.mbank;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import pl.net.newton.Makler.R;
import pl.net.newton.Makler.common.DateFormatUtils;
import pl.net.newton.Makler.common.NumberFormatUtils;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolBuilder;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.gpw.DefaultQuotesReceiver;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import pl.net.newton.Makler.gpw.model.Finances;
import pl.net.newton.Makler.gpw.model.OrderState;
import pl.net.newton.Makler.gpw.model.Order.LimitType;
import pl.net.newton.Makler.mbank.model.MbankOrder;
import pl.net.newton.Makler.mbank.model.MbankPaper;
import pl.net.newton.Makler.mbank.model.MbankQuote;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

public class MbankClient {
	private static final String TAG = "Makler";

	private String login;

	private String password;

	private MbankConnector conn;

	private Context ctx;

	private SymbolsDb symbolsDb;

	private List<MbankPaper> papers;

	private Map<String, MbankPaper> paperBySymbol;

	private Map<String, MbankPaper> paperByCode;

	private static final String papersConfField = "last_mBankPapersUpdated";

	private static final String papersFile = "papers_mBank";

	private static final String quotesHost = "notowania.mbank.com.pl";

	private static final Pattern LABEL_REGEXP = Pattern.compile("StockName\">([^<]+)</");

	private static final Pattern AMOUNT_REGEXP = Pattern.compile("Amount\">([^<]+)</");

	private String disablePassNo = null;

	public MbankClient(Context ctx, String login, String password, SymbolsDb db) {
		this.ctx = ctx;
		this.login = login;
		this.password = password;
		this.conn = new MbankConnector("www.mbank.com.pl", 443);
		this.symbolsDb = db;
	}

	public void login() throws GpwException, InvalidPasswordException {
		conn.login(login, password);
		if ((papers == null || papers.isEmpty()) && !loadPapers())
			getPapers(true);
	}

	public void logout() throws GpwException {
		conn.sendCommand("/logout.aspx", null, null, true, null, true);
	}

	public Boolean loggedIn() {
		String page = null;
		try {
			page = conn.sendCommand("/accounts_list.aspx", null, null, true, null, true);
		} catch (Exception e) {
			return false;
		}
		return page.contains("Dostępne rachunki");
	}

	public MbankPaper getPaperBySymbol(String symbol) {
		return paperBySymbol.get(symbol);
	}

	public MbankPaper getPaperByCode(String code) {
		return paperByCode.get(code);
	}

	public List<MbankQuote> getQuotes(String symbol) throws GpwException {
		List<MbankPaper> papers = new ArrayList<MbankPaper>();
		for (String s : symbol.split(","))
			papers.add(paperBySymbol.get(s));
		return getQuotes(papers);
	}

	public List<MbankQuote> getQuotesBySymbols(List<String> symbols) throws GpwException {
		List<MbankPaper> papers = new ArrayList<MbankPaper>();
		for (String s : symbols)
			papers.add(paperBySymbol.get(s));
		return getQuotes(papers);
	}

	public List<MbankQuote> getQuotes(List<MbankPaper> papers) throws GpwException {
		Document doc = conn.getBuilder().newDocument();
		Element dataset = doc.createElement("dataset");
		dataset.setAttribute("name", "pRdfPapiery");
		dataset.setAttribute("ileOfert", "1");
		dataset.setAttribute("pac", ctx.getString(R.string.zero));
		dataset.setAttribute("pact", ctx.getString(R.string.zero));
		dataset.setAttribute("paco", ctx.getString(R.string.zero));
		for (MbankPaper p : papers) {
			if (p != null) {
				Element pEl = doc.createElement("pRdfPapiery");
				pEl.setAttribute("isin", p.getCode());
				dataset.appendChild(pEl);
			}
		}
		Document xmlDoc = null;
		for (int i = 0; i < 3 && xmlDoc == null; i++) {
			try {
				xmlDoc = conn.sendXml("/ntwweb/epromak/notowbuf", "selectRdf", dataset, null, quotesHost,
						null);
			} catch (NullPointerException e) {
				throw new GpwException(e);
			}
		}
		if (xmlDoc == null) {
			logout();
			throw new GpwException("Błąd w pobraniu notowań");
		}
		NodeList data = xmlDoc.getElementsByTagName("dataset").item(0).getChildNodes();
		Map<String, String> quoteHash = new HashMap<String, String>();
		for (int i = 0, j = data.getLength(); i < j; i++) {
			Element r = (Element) data.item(i);
			quoteHash.put(r.getAttribute("isin"), r.getAttribute("data"));
		}
		List<MbankQuote> quotes = new ArrayList<MbankQuote>();
		for (MbankPaper p : papers) {
			if (p != null)
				quotes.add(new MbankQuote(quoteHash.get(p.getCode()), symbolsDb.getSymbolBySymbol(p
						.getSymbol())));
			else
				quotes.add(null);
		}
		return quotes;
	}

	public List<MbankPaper> getPapers(Boolean forceReload) throws GpwException {
		if (papers != null && !forceReload)
			return papers;
		DefaultQuotesReceiver newtonQuotes = new DefaultQuotesReceiver(ctx);
		List<Symbol> symbols = newtonQuotes.getSymbols();
		Map<String, Symbol> symbolByCode = new Hashtable<String, Symbol>();
		Map<String, Symbol> symbolByName = new Hashtable<String, Symbol>();

		for (Symbol s : symbols) {
			symbolByCode.put(s.getCode(), s);
			symbolByName.put(s.getName(), s);
		}

		String page;
		page = conn.sendCommand("/accounts_list.aspx", null, null, true, null, true);
		page = conn.sendCommand("/investitions.aspx", null, null, false, null, true);
		page = conn.sendCommand("/di/di_quotation_basket_list.aspx", null,
				MbankConnector.getParameters(page, "/di/di_quotation_basket_list.aspx"), false, null, true);
		page = conn.sendCommand("/di/di_quotation_basket_edit.aspx", null, null, false, null, true);
		if (page.contains("nie posiadasz aktywnej"))
			throw new GpwException("Nie posiadasz aktywnej usługi notowań ciągłych.");

		Pattern p = Pattern.compile("value=\"([^\"]+)\"\\>([^\\<]+)\\</option\\>");
		Matcher m = p.matcher(page);
		papers = new ArrayList<MbankPaper>();
		while (m.find()) {
			String code = m.group(1).substring(3);
			String trimmedName = m.group(2).split(ctx.getString(R.string.minus))[0];
			if (symbolByCode.containsKey(code)) {
				Symbol s = symbolByCode.get(code);
				papers.add(new MbankPaper(s.getSymbol(), m.group(2), false, code));
			} else if (symbolByName.containsKey(trimmedName)) {
				Symbol s = symbolByName.get(trimmedName);
				papers.add(new MbankPaper(s.getSymbol(), m.group(2), false, s.getCode()));
			} else
				Log.w(TAG, new StringBuilder("nieznany papier ").append(code).append(' ').append(m.group(2))
						.toString());
		}
		for (String c : new String[] { "PL9999999995", "PL9999999987", "PL9999999912", "PL9999999979",
				"PL9999999565" })
			if (symbolByCode.containsKey(c)) {
				Symbol s = symbolByCode.get(c);
				papers.add(new MbankPaper(s.getSymbol(), s.getName(), true, c));

			}
		savePapers();
		createCachePapers();
		return papers;
	}

	public void trade(MbankOrder o) throws GpwException {
		if (o.getType() == 'K')
			buy(o);
		else
			sell(o);
	}

	public String paperInfo(String name) throws GpwException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("PA", "di_online_buy_order"));
		params.add(new BasicNameValuePair("Query", name));

		String paperData = "";
		for (String ddlShareType : new String[] { "AKCJA", "CERTYFIKAT", "OBLIGACJA", "PRAWO_POBORU",
				"WARRANT" }) {
			modifyParams(params, "ddlShareType", ddlShareType);
			paperData = conn.sendCommand("/di/di_online_buy_order.aspx", null, params, false, null, false);
			if (paperData != null && paperData.length() > 0)
				break;
		}
		Log.d(TAG, paperData);
		return paperData;
	}

	public void buy(MbankOrder o) throws GpwException {
		String shareType, paperData;
		paperData = paperInfo(o.getPaper().getName());
		if (paperData.equals(""))
			return;

		String[] paperDataAr;
		// String paperId;

		paperDataAr = paperData.split(",");
		paperData = paperDataAr[0];
		// paperId = paperDataAr[1];
		paperDataAr = paperData.split("@");
		shareType = paperDataAr[2];

		List<NameValuePair> params = o.getParams();
		params.add(new BasicNameValuePair("tbacShareId_selectedKey", paperData));
		params.add(new BasicNameValuePair("ddlShareType", shareType));
		params.add(new BasicNameValuePair("__CurrentWizardStep", "1"));
		params.add(new BasicNameValuePair("authTurnOff", "off"));
		params.add(new BasicNameValuePair("chbDIMoreParameters", "on"));
		params.add(new BasicNameValuePair("Derivatives", "rbShareName"));

		String page;
		page = conn.sendCommand("/accounts_list.aspx", null, null, true, null, true);
		page = conn.sendCommand("/investitions.aspx", null, null, false, null, true);
		page = conn.sendCommand("/di/di_paper_list.aspx", null,
				MbankConnector.getParameters(page, "/di/di_paper_list.aspx"), false, null, true);
		page = conn.sendCommand("/di/di_online_buy_order.aspx", null,
				MbankConnector.getParameters(page, "/di/di_online_buy_order.aspx", "Kupno"), false, null,
				true);
		params.add(new BasicNameValuePair("lblStockPurchaseResources", MbankConnector.getHiddenFieldValue(
				page, "lblStockPurchaseResources")));
		params.add(new BasicNameValuePair("lblBondsPurchaseResources", MbankConnector.getHiddenFieldValue(
				page, "lblBondsPurchaseResources")));
		params.add(new BasicNameValuePair("lblDerivativeResources", MbankConnector.getHiddenFieldValue(page,
				"lblDerivativeResources")));

		List<NameValuePair> buyParam = MbankConnector.getParameters(page, "/di/di_online_buy_order.aspx",
				"Dalej");
		buyParam.addAll(params);
		page = conn.sendCommand("/di/di_online_buy_order.aspx", null, buyParam, false, null, true);

		modifyParams(params, "__CurrentWizardStep", "2");
		modifyParams(params, "__PARAMETERS",
				MbankConnector.getParameters(page, "/di/di_online_buy_order.aspx", "Zatwie").get(0)
						.getValue());
		params.add(new BasicNameValuePair("lblShareCode", paperDataAr[0]));
		params.add(new BasicNameValuePair("lblOrderMarket", paperDataAr[1]));
		params.add(new BasicNameValuePair("chbDefaultPage", "off"));
		conn.sendCommand("/di/di_online_buy_order.aspx", null, params, false, null, true);
	}

	public void sell(MbankOrder o) throws GpwException {
		String shareType, paperData;
		paperData = paperInfo(o.getPaper().getName());
		if (paperData.equals(""))
			return;

		if (paperData.contains("AKCJA"))
			shareType = "A";
		else
			shareType = "O";

		String page;
		page = conn.sendCommand("/accounts_list.aspx", null, null, true, null, true);
		page = conn.sendCommand("/investitions.aspx", null, null, false, null, true);
		page = conn.sendCommand("/di/di_paper_list.aspx", null,
				MbankConnector.getParameters(page, "/di/di_paper_list.aspx"), false, null, true);
		page = conn.sendCommand("/di/di_online_sell_order.aspx", null,
				MbankConnector.getParameters(page, "/di/di_online_sell_order.aspx", "Sprzeda"), false, null,
				true);
		if (shareType.equals("O"))
			page = conn
					.sendCommand("/di/di_online_sell_order.aspx", null, MbankConnector.getParameters(page,
							"/di/di_online_sell_order.aspx", "Sprzeda. obligacji"), false, null, true);

		List<NameValuePair> params = o.getParams();
		params.add(new BasicNameValuePair("ShareType", shareType));
		params.add(new BasicNameValuePair("__CurrentWizardStep", "1"));
		params.add(new BasicNameValuePair("authTurnOff", "off"));

		{
			String regexp = new StringBuilder("value=\"([^@]+@[^@]+@").append(o.getPaper().getCode())
					.append("@[^\"]+)").toString();
			Pattern p = Pattern.compile(regexp);
			Matcher m = p.matcher(page);
			if (m.find()) {
				params.add(new BasicNameValuePair("lbxShareId", m.group(1)));
			} else {
				return;
			}
		}

		List<NameValuePair> sellParam = MbankConnector.getParameters(page, "/di/di_online_sell_order.aspx",
				"Dalej");
		sellParam.addAll(params);
		page = conn.sendCommand("/di/di_online_sell_order.aspx", null, sellParam, false, null, true);

		modifyParams(params, "__CurrentWizardStep", "2");
		modifyParams(params, "__PARAMETERS",
				MbankConnector.getParameters(page, "/di/di_online_sell_order.aspx", "Zatwie").get(0)
						.getValue());
		conn.sendCommand("/di/di_online_sell_order.aspx", null, params, false, null, true);
	}

	public Boolean change(String id, MbankOrder order) throws GpwException {
		Log.d(TAG, "changing order " + id);
		String page;
		page = conn.sendCommand("/accounts_list.aspx", null, null, true, null, true);
		page = conn.sendCommand("/investitions.aspx", null, null, false, null, true);
		page = conn.sendCommand("/di/di_paper_list.aspx", null,
				MbankConnector.getParameters(page, "/di/di_paper_list.aspx"), false, null, true);
		page = conn.sendCommand("/di/di_online_order_history.aspx", null,
				MbankConnector.getParameters(page, "/di/di_online_order_history.aspx"), false, null, true);

		String regexp = new StringBuilder(id).append("(\\<[^\\<]+){28}").toString();
		Pattern p = Pattern.compile(regexp);
		Matcher m = p.matcher(page);
		if (!m.find())
			return false;
		List<NameValuePair> params = MbankConnector.getParameters(m.group(0),
				"/di/di_online_order_modify.aspx");
		if (params == null)
			return false;

		page = conn.sendCommand("/di/di_online_order_modify.aspx", null, params, false, null, true);
		params = MbankConnector.getParameters(page, "/di/di_online_order_modify.aspx", "Dalej");
		for (String name : new String[] { "lblOrderTime", "cDisposalID", "cOrderID", "lblOrderType", "cName",
				"cISINCode", "lblOrderStatus", "authTurnOff", "__CurrentWizardStep", "dtdOrderDate_year",
				"dtdOrderDate_month", "dtdOrderDate_day", "dtdSessionDate_year", "dtdSessionDate_month",
				"dtdSessionDate_day", "mPriceLimit_Curr", "mOrderValue", "mOrderValue_Curr" })
			params.add(new BasicNameValuePair(name, MbankConnector.getHiddenFieldValue(page, name)));

		params.addAll(order.getChangeParams());
		page = conn.sendCommand("/di/di_online_order_modify.aspx", null, params, false, null, true);
		modifyParams(params, "__CurrentWizardStep", "2");
		modifyParams(params, "__PARAMETERS",
				MbankConnector.getParameters(page, "/di/di_online_order_modify.aspx", "Dalej").get(0)
						.getValue());
		params.add(new BasicNameValuePair("lblProvision", MbankConnector.getHiddenFieldValue(page,
				"lblProvision")));
		params.add(new BasicNameValuePair("tbActLimit_Curr", MbankConnector.getHiddenFieldValue(page,
				"tbActLimit_Curr")));
		page = conn.sendCommand("/di/di_online_order_modify.aspx", null, params, false, null, true);

		modifyParams(params, "__PARAMETERS",
				MbankConnector.getParameters(page, "/di/di_online_order_modify.aspx", "Zatwie").get(0)
						.getValue());
		page = conn.sendCommand("/di/di_online_order_modify.aspx", null, params, false, null, true);

		return true;
	}

	public Boolean cancel(String id) throws GpwException {
		Log.d(TAG, "cancelling order " + id);
		String page;
		page = conn.sendCommand("/accounts_list.aspx", null, null, true, null, true);
		page = conn.sendCommand("/investitions.aspx", null, null, false, null, true);
		page = conn.sendCommand("/di/di_paper_list.aspx", null,
				MbankConnector.getParameters(page, "/di/di_paper_list.aspx"), false, null, true);
		page = conn.sendCommand("/di/di_online_order_history.aspx", null,
				MbankConnector.getParameters(page, "/di/di_online_order_history.aspx"), false, null, true);

		String regexp = new StringBuilder(id).append("(\\<[^\\<]+){31}").toString();
		Pattern p = Pattern.compile(regexp);
		Matcher m = p.matcher(page);
		if (!m.find())
			return false;
		List<NameValuePair> params = MbankConnector.getParameters(m.group(0),
				"/di/di_online_order_cancel.aspx");
		if (params == null)
			return false;

		page = conn.sendCommand("/di/di_online_order_cancel.aspx", null, params, false, null, true);
		params = MbankConnector.getParameters(page, "/di/di_online_order_cancel.aspx", "Zatwie");
		for (String name : new String[] { "cRynek", "cOrderID", "cDisposalID", "cName", "cISINCode",
				"authTurnOff", "__CurrentWizardStep" })
			params.add(new BasicNameValuePair(name, MbankConnector.getHiddenFieldValue(page, name)));
		page = conn.sendCommand("/di/di_online_order_cancel.aspx", null, params, false, null, true);
		return true;
	}

	public List<OrderState> getOrderStates() throws GpwException {
		List<OrderState> states = new ArrayList<OrderState>();

		String page;
		page = conn.sendCommand("/accounts_list.aspx", null, null, true, null, true);
		page = conn.sendCommand("/investitions.aspx", null, null, false, null, true);
		page = conn.sendCommand("/di/di_paper_list.aspx", null,
				MbankConnector.getParameters(page, "/di/di_paper_list.aspx"), false, null, true);
		page = conn.sendCommand("/di/di_online_order_history.aspx", null,
				MbankConnector.getParameters(page, "/di/di_online_order_history.aspx"), false, null, true);

		String regexp = new StringBuilder("doSubmit\\('").append("/di/di_online_order_details.aspx")
				.append("','[^']*','[^']*','([^']*)'").toString();
		Pattern p = Pattern.compile(regexp);
		Matcher m = p.matcher(page);

		String field = "label class=\"label\"\\>([^\\<]+)\\</label\\>\\<div class=\"content\"\\>([^\\<]+)";
		Pattern fieldP = Pattern.compile(field);
		while (m.find()) {
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("__PARAMETERS", m.group(1)));
			page = conn.sendCommand("/di/di_online_order_details.aspx", null, params, false, null, true);

			Matcher fieldM = fieldP.matcher(page);
			Map<String, String> state = new HashMap<String, String>();
			while (fieldM.find()) {
				String name, value;
				name = fieldM.group(1);
				value = fieldM.group(2).trim();
				state.put(name, value);
			}

			BigDecimal limit = null;
			BigDecimal limitAkt = null;
			try {
				limit = new BigDecimal(state.get("Limit ceny").replace(',', '.').replace(" ", "")
						.replace("PLN", ""));
			} catch (Exception e) {
			} // it's ok
			try {
				limitAkt = new BigDecimal(state.get("Limit aktywacji").replace(',', '.').replace(" ", "")
						.replace("PLN", ""));
			} catch (Exception e) {
			} // it's ok

			LimitType lType = null;
			try {
				lType = LimitType.valueOf(state.get("Limit ceny"));
			} catch (Exception e) {
			}

			MbankOrder o = new MbankOrder(state.get("Rodzaj zlecenia").equals("KUPNO") ? 'K' : 'S',
					paperByCode.get(state.get("Kod papieru")), Integer.parseInt(state.get("Liczba zlecona")
							.replace(" ", "")), limit, lType, limitAkt, swapDate(state.get("Data sesji")),
					swapDate(state.get("Ważność zlecenia:")), null, null, null, null);
			String wyk = state.get("Liczba wykonana");
			Integer liczbaWyk = 0;
			if (wyk != null)
				liczbaWyk = Integer.parseInt(wyk.replace(" ", ""));
			OrderState s = new OrderState(o.getDBOrder(ctx, symbolsDb), state.get("Numer zlecenia"),
					state.get("Status"), liczbaWyk);
			states.add(s);
		}
		return states;
	}

	public Finances getFinances() throws GpwException {
		String page;
		page = conn.sendCommand("/accounts_list.aspx", null, null, true, null, true);
		page = conn.sendCommand("/investitions.aspx", null, null, false, null, true);
		page = conn.sendCommand("/di/di_paper_list.aspx", null,
				MbankConnector.getParameters(page, "/di/di_paper_list.aspx"), false, null, true);

		String regexp = "title=\"Szczeg[^\\>]+>([^\\<]+)([^\\>]+\\>){8}(\\d+)([^\\>]+\\>){4}(\\d+)([^\\>]+\\>){6}([^P]+)";
		Pattern p = Pattern.compile(regexp);
		Matcher m = p.matcher(page);
		List<pl.net.newton.Makler.gpw.model.Paper> papers = new ArrayList<pl.net.newton.Makler.gpw.model.Paper>();
		Boolean detailsFailed = false;
		while (m.find()) {
			Symbol s = null;
			if (!detailsFailed) {
				try {
					String detailsPage = conn.sendCommand("/di/di_paper_details.aspx", null,
							MbankConnector.getParameters(page, "/di/di_paper_details.aspx", m.group(1)),
							false, null, true);
					String detailsRegexp = "label class=\"label\"\\>Kod papieru\\</label\\>\\<div class=\"content\"\\>([^\\<]+)";
					Pattern detailsP = Pattern.compile(detailsRegexp);
					Matcher detailsM = detailsP.matcher(detailsPage);
					if (detailsM.find())
						s = symbolsDb
								.getSymbolBySymbol(paperByCode.get(detailsM.group(1).trim()).getSymbol());

					page = conn.sendCommand("/di/di_paper_list.aspx", null,
							MbankConnector.getParameters(page, "/di/di_paper_list.aspx", "&nbsp;Powrót"),
							false, null, true);
				} catch (Exception e) {
					detailsFailed = true;
					Log.d(TAG, e.toString());
				}
			}
			if (s == null && this.papers != null) {
				for (MbankPaper _p : this.papers)
					if (_p.getName().startsWith(m.group(1))) {
						s = symbolsDb.getSymbolBySymbol(_p.getSymbol());
						break;
					}
			}
			if (s == null)
				s = new SymbolBuilder().setSymbol("???").setName(m.group(1)).build();

			Integer quantity = Integer.parseInt(m.group(3)) + Integer.parseInt(m.group(5));
			BigDecimal quote = new BigDecimal(m.group(7).replace(',', '.').replace(" ", ""))
					.divide(new BigDecimal(quantity));
			pl.net.newton.Makler.gpw.model.Paper paper = new pl.net.newton.Makler.gpw.model.Paper(s,
					quantity, quantity, quote);
			papers.add(paper);
		}
		Map<String, BigDecimal> extracted = financesExtract(page);
		return new Finances(papers, extracted.get("Środki dostępne dla zleceń kupna akcji"),
				extracted.get("Saldo"), extracted.get("Suma należności"));
	}

	public boolean disablePassword() throws GpwException {
		String page;
		page = conn.sendCommand("/accounts_list.aspx", null, null, true, null, true);
		page = conn.sendCommand("/investitions.aspx", null, null, false, null, true);
		page = conn.sendCommand("/di/di_service_details.aspx.aspx", null,
				MbankConnector.getParameters(page, "/di/di_service_details.aspx.aspx"), false, null, true);
		page = conn.sendCommand("/di/di_authorization_turnoff.aspx", null,
				MbankConnector.getParameters(page, "/di/di_authorization_turnoff.aspx"), false, null, true);
		if (page.contains("zostało już dzisiaj wyłączone"))
			return true;

		List<NameValuePair> params = MbankConnector.getParameters(page, "/di/di_authorization_turnoff.aspx",
				"Dalej");
		params.add(new BasicNameValuePair("authTurnOff", "off"));
		params.add(new BasicNameValuePair("__CurrentWizardStep", "1"));
		page = conn.sendCommand("/di/di_authorization_turnoff.aspx", null, params, false, null, true);

		modifyParams(params, "__CurrentWizardStep", "2");
		modifyParams(params, "__PARAMETERS",
				MbankConnector.getParameters(page, "/di/di_paper_list.aspx", "Potwie").get(0).getValue());
		params.add(new BasicNameValuePair("TransactionType", MbankConnector.getHiddenFieldValue(page,
				"TransactionType")));

		disablePassNo = MbankConnector.matchGroup("smsowe dla operacji (\\d+) ", page);
		page = conn.sendCommand("/di/di_paper_list.aspx", null, params, false, null, true);
		Log.d(TAG, new StringBuilder("operacja ").append(disablePassNo).toString());
		return false;
	}

	public boolean disablePassword(String code) throws GpwException {
		String page;
		page = conn.sendCommand("/accounts_list.aspx", null, null, true, null, true);
		page = conn.sendCommand("/suspended_transaction_list.aspx", null, null, true, null, true);
		Pattern p = Pattern.compile(new StringBuilder("suspended_transaction_list_grid[^\\>]+\\>")
				.append(disablePassNo).append("(\\<[^\\<]+){12}").toString());
		Matcher m = p.matcher(page);
		if (!m.find())
			return false;
		page = conn.sendCommand("/suspended_transaction_handling.aspx", null,
				MbankConnector.getParameters(m.group(1), "/suspended_transaction_handling.aspx"), false,
				null, true);

		List<NameValuePair> params = MbankConnector.getParameters(page,
				"/suspended_transaction_handling.aspx", "Zatwie");
		params.add(new BasicNameValuePair("__CurrentWizardStep", "1"));
		params.add(new BasicNameValuePair("TransactionType", MbankConnector.getHiddenFieldValue(page,
				"TransactionType")));
		params.add(new BasicNameValuePair("tbOperationType", MbankConnector.getHiddenFieldValue(page,
				"tbOperationType")));
		params.add(new BasicNameValuePair("tbOperationDesc", MbankConnector.getHiddenFieldValue(page,
				"tbOperationDesc")));
		params.add(new BasicNameValuePair("authCode", code));
		page = conn.sendCommand("/suspended_transaction_handling.aspx", null, params, false, null, true);

		return page.contains("Operacja wykonana poprawnie");
	}

	private void createCachePapers() {
		paperBySymbol = new Hashtable<String, MbankPaper>();
		paperByCode = new Hashtable<String, MbankPaper>();
		for (MbankPaper p : papers) {
			paperByCode.put(p.getCode(), p);
			paperBySymbol.put(p.getSymbol(), p);
		}
	}

	private Boolean loadPapers() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String lastUpdated = prefs.getString(papersConfField, "0000-00-00");

		if (!lastUpdated.equals(DateFormatUtils.formatCurrentDate()))
			return false;
		try {
			FileInputStream fis = ctx.openFileInput(papersFile);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ObjectInputStream oin = new ObjectInputStream(bis);
			Integer count = oin.readInt();
			papers = new ArrayList<MbankPaper>();
			for (int i = 0; i < count; i++) {
				MbankPaper p = new MbankPaper(oin.readUTF());
				papers.add(p);
			}
			oin.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		if (papers == null || papers.isEmpty())
			return false;
		createCachePapers();
		Log.d(TAG, "załadowano papiery");
		return true;
	}

	private void savePapers() {
		try {
			FileOutputStream fos = ctx.openFileOutput(papersFile, Context.MODE_PRIVATE);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream oout = new ObjectOutputStream(bos);
			oout.writeInt(papers.size());
			for (MbankPaper p : papers)
				oout.writeUTF(p.getSerializedString());
			oout.close();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		Editor edit = prefs.edit();
		edit.putString(papersConfField, DateFormatUtils.formatCurrentDate());
		edit.commit();
		Log.d(TAG, "zapisano papiero");
	}

	public static void modifyParams(List<NameValuePair> l, String name, String newValue) {
		NameValuePair pair = new BasicNameValuePair(name, newValue);
		for (int i = 0; i < l.size(); i++)
			if (l.get(i).getName().equals(name)) {
				l.set(i, pair);
				return;
			}
		l.add(pair);
	}

	private static Map<String, BigDecimal> financesExtract(String page) {
		Matcher m = LABEL_REGEXP.matcher(page);

		List<String> labels = new ArrayList<String>();
		while (m.find()) {
			labels.add(m.group(1));
		}
		labels.add("Saldo");

		m = AMOUNT_REGEXP.matcher(page);
		Map<String, BigDecimal> finances = new HashMap<String, BigDecimal>();
		int i = 0;
		while (m.find() && i < labels.size()) {
			String label = labels.get(i++);
			String value = m.group(1);
			BigDecimal parsed = NumberFormatUtils.parseOrNull(value);
			finances.put(label, parsed);
			Log.d(TAG, label + ": " + parsed);
		}
		return finances;
	}

	private static String swapDate(String date) {
		if (date == null) {
			return null;
		}
		String[] d = date.split("-");
		if (d.length == 3) {
			return new StringBuilder(d[2]).append('-').append(d[1]).append('-').append(d[0]).toString();
		} else {
			return d[0];
		}
	}
}