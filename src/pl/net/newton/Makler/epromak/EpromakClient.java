package pl.net.newton.Makler.epromak;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import pl.net.newton.Makler.common.DataSource;
import pl.net.newton.Makler.common.DateFormatUtils;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.gpw.DefaultQuotesReceiver;
import pl.net.newton.Makler.gpw.ex.GpwException;
import pl.net.newton.Makler.gpw.ex.InvalidPasswordException;
import pl.net.newton.Makler.epromak.model.EpromakFinance;
import pl.net.newton.Makler.epromak.model.EpromakOrder;
import pl.net.newton.Makler.epromak.model.EpromakOrderState;
import pl.net.newton.Makler.epromak.model.EpromakOwnership;
import pl.net.newton.Makler.epromak.model.EpromakPaper;
import pl.net.newton.Makler.epromak.model.EpromakQuote;
import pl.net.newton.Makler.epromak.model.EpromakRoundRule;

public class EpromakClient {
	private static final String TAG = "Makler";

	private EpromakConnector conn;

	private List<EpromakPaper> papers = null;

	private List<EpromakRoundRule> roundRules = null;

	private Map<String, EpromakPaper> paperBySymbol;

	private Map<String, EpromakPaper> paperByCode;

	private String papersSchema;

	private final String papersFile;

	private final String papersConfField;

	private Context ctx;

	private DataSource type;

	public EpromakClient(Context ctx, String name, String password, DataSource dataSourceType) {
		this.ctx = ctx;
		this.type = dataSourceType;
		this.papersFile = "papers_" + dataSourceType;
		this.papersConfField = "papersUpdated_" + dataSourceType;

		String host = dataSourceType.getHostname();
		boolean trust = false;
		if (EnumSet.of(DataSource.pkobp, DataSource.bre, DataSource.bdm).contains(dataSourceType)) {
			trust = true;
		}

		conn = new EpromakConnector(name, password, host, trust, type);
	}

	public void login() throws GpwException, InvalidPasswordException {
		conn.login();
		if ((papers == null || papers.isEmpty()) && !loadPapers())
			getPapers(true);
	}

	public Boolean loggedIn() {
		try {
			Document doc = conn.getBuilder().newDocument();
			Element dataset = doc.createElement("dataset");
			dataset.setAttribute("name", "pRdfPapiery");
			dataset.setAttribute("ileOfert", "1");
			conn.sendXml("/epmntw/epromak/notowbuf", "selectRdf", dataset, null);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public Boolean keepAlive() {
		return conn.keepAlive();
	}

	public void logout() throws GpwException, InvalidPasswordException {
		HttpEntity d;
		try {
			d = new StringEntity("logout");
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "can't logout", e);
			return;
		}
		conn.sendCommand("/epmntw/epromak/logowanie", null, d, false, null);

		conn.sendXml("/epmpow/epromak/logowanie", "logout", "", null);
		conn.sendXml("/epmntw/epromak/logowanie", "logout", "", null);

		conn.sendCommand("/epromak/epromak/logout", null, null, false, null);
	}

	public List<EpromakPaper> getPapers(Boolean forceReload) throws GpwException {
		if (forceReload || papers == null || papers.isEmpty()) {
			HttpEntity en = conn.sendCommand("/epromak/epromak/getdata/sKatalogPapierow/lista", null, null,
					true, null);
			Document doc = null;
			try {
				doc = conn.getBuilder().parse(en.getContent());
				/*
				 * if(true) { FileInputStream is = new FileInputStream("/sdcard/papers-bre"); doc =
				 * conn.getBuilder().parse(is); is.close(); }
				 */
				if (EpromakConnector.debug)
					EpromakConnector.logToFile(" << "
							+ pl.net.newton.Makler.httpClient.Connector.getStringFromNode(doc
									.getDocumentElement()));
			} catch (Exception e) {
				throw new GpwException(e);
			}
			if (doc == null)
				return null;
			Element dataset = (Element) doc.getElementsByTagName("dataset").item(0);
			NodeList list = dataset.getChildNodes();
			papersSchema = dataset.getAttribute("schema");
			String[] fields = papersSchema.split(";");

			papers = new ArrayList<EpromakPaper>();
			if (type == DataSource.bre) {
				int nameField = -1;
				for (int i = 0; i < fields.length; i++)
					if (fields[i].equals("skrot")) {
						nameField = i;
						break;
					}
				DefaultQuotesReceiver n = new DefaultQuotesReceiver(ctx);
				Map<String, Symbol> newtonSymbolByCode = new Hashtable<String, Symbol>();
				for (Symbol s : n.getSymbols())
					newtonSymbolByCode.put(s.getCode(), s);
				for (int i = 0, j = list.getLength(); i < j; i++) {
					String data = ((Element) list.item(i)).getAttribute("v");
					EpromakPaper p = new EpromakPaper(data, fields);
					if (newtonSymbolByCode.containsKey(p.getCode())) {
						String d[] = data.split(";");
						d[nameField] = p.getName() + " " + newtonSymbolByCode.get(p.getCode()).getSymbol();
						p = new EpromakPaper(d, fields);
					}
					papers.add(p);
				}
			} else {
				for (int i = 0, j = list.getLength(); i < j; i++) {
					String s = ((Element) list.item(i)).getAttribute("v");
					EpromakPaper p = new EpromakPaper(s, fields);
					papers.add(p);
				}
			}
			createCachePapers();
			Log.d(TAG, "ściągnięto papiery");
			savePapers();
		}
		return papers;
	}

	private void createCachePapers() {
		paperBySymbol = new Hashtable<String, EpromakPaper>();
		paperByCode = new Hashtable<String, EpromakPaper>();
		for (EpromakPaper p : papers) {
			paperByCode.put(p.getCode(), p);
			paperBySymbol.put(p.getSymbol(), p);
		}
	}

	public EpromakPaper getPaperBySymbol(String symbol) {
		return paperBySymbol.get(symbol);
	}

	public EpromakPaper getPaperByCode(String code) {
		return paperByCode.get(code);
	}

	private Boolean loadPapers() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String lastUpdated = prefs.getString(papersConfField, "0000-00-00");

		if (!lastUpdated.equals(DateFormatUtils.formatYyyyMmDd()))
			return false;
		try {
			FileInputStream fis = ctx.openFileInput(papersFile);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ObjectInputStream oin = new ObjectInputStream(bis);
			papersSchema = oin.readUTF();
			String fields[] = papersSchema.split(";");
			Integer count = oin.readInt();
			papers = new ArrayList<EpromakPaper>();
			for (int i = 0; i < count; i++) {
				EpromakPaper p = new EpromakPaper(oin.readUTF(), fields);
				papers.add(p);
			}
			oin.close();
		} catch (Exception e) {
			Log.e(TAG, "can't load papers", e);
			return false;
		}
		createCachePapers();
		Log.d(TAG, "załadowano papiery");
		return true;
	}

	private void savePapers() {
		try {
			FileOutputStream fos = ctx.openFileOutput(papersFile, Context.MODE_PRIVATE);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream oout = new ObjectOutputStream(bos);
			oout.writeUTF(papersSchema);
			oout.writeInt(papers.size());
			for (EpromakPaper p : papers)
				oout.writeUTF(p.getSerializedString());
			oout.close();
		} catch (Exception e) {
			Log.e(TAG, "can't save papers", e);
			return;
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		Editor edit = prefs.edit();
		edit.putString(papersConfField, DateFormatUtils.formatYyyyMmDd());
		edit.commit();
		Log.d(TAG, "zapisano papiero");
	}

	public List<EpromakQuote> getQuotes(List<EpromakPaper> papers) throws GpwException, InvalidPasswordException {
		Document doc = conn.getBuilder().newDocument();
		Element dataset = doc.createElement("dataset");
		dataset.setAttribute("name", "pRdfPapiery");
		dataset.setAttribute("ileOfert", "1");
		Element res = (Element) conn.sendXml("/epmntw/epromak/notowbuf", "selectRdf", dataset, null)
				.getElementsByTagName("dataset").item(0);
		dataset.setAttribute("pac", res.getAttribute("pac"));
		dataset.setAttribute("pact", res.getAttribute("pac"));
		dataset.setAttribute("paco", res.getAttribute("pac"));
		for (EpromakPaper p : papers) {
			if (p != null) {
				Element pEl = doc.createElement("pRdfPapiery");
				pEl.setAttribute("isin", p.getCode());
				dataset.appendChild(pEl);
			}
		}
		NodeList data = conn.sendXml("/epmntw/epromak/notowbuf", "selectRdf", dataset, null)
				.getElementsByTagName("dataset").item(0).getChildNodes();
		Map<String, String> quoteHash = new HashMap<String, String>();
		for (int i = 0, j = data.getLength(); i < j; i++) {
			Element r = (Element) data.item(i);
			quoteHash.put(r.getAttribute("isin"), r.getAttribute("data"));
		}
		List<EpromakQuote> quotes = new ArrayList<EpromakQuote>();
		for (EpromakPaper p : papers)
			if (p == null)
				quotes.add(null);
			else
				quotes.add(new EpromakQuote(quoteHash.get(p.getCode()), p));
		return quotes;
	}

	public List<EpromakQuote> getQuotesFromSymbols(List<String> symbols) throws GpwException,
			InvalidPasswordException {
		List<EpromakPaper> papers = new ArrayList<EpromakPaper>();
		for (String s : symbols) {
			EpromakPaper p = getPaperBySymbol(s);
			papers.add(p);
		}
		return getQuotes(papers);
	}

	public List<EpromakQuote> getQuotes(String symbols) throws GpwException, InvalidPasswordException {
		return getQuotesFromSymbols(Arrays.asList(symbols.split(",")));
	}

	public EpromakFinance getFinance() throws GpwException, InvalidPasswordException {
		Document doc = conn.getBuilder().newDocument();
		Element dataset = doc.createElement("dataset");
		dataset.setAttribute("name", "pSrodkiFinansoweUmowy");
		Element e = doc.createElement("pSrodkiFinansoweUmowy");
		
		String[] fields;
		if(type == DataSource.millennium) {
			fields = EpromakFinance.FIELDS_MILLENNIUM;
		} else {
			fields = EpromakFinance.FIELDS;
		}
		
		for (int i = 0; i < fields.length; i++) {
			e.setAttribute(fields[i], "");
		}
		Document data = conn.sendXml("/epromak/epromak/dbwrite", "select", dataset, null);
		try {
			return new EpromakFinance((Element) data.getElementsByTagName("pSrodkiFinansoweUmowy").item(0));
		} catch(NullPointerException ex) {
			return EpromakFinance.getEmpty();
		}
	}

	public List<EpromakOwnership> getOwnerships() throws GpwException, InvalidPasswordException {
		Document doc = conn.getBuilder().newDocument();
		Element dataset = doc.createElement("dataset");
		dataset.setAttribute("name", "pSaldaPW");
		dataset.setAttribute("ptfwej", "");
		Document data = conn.sendXml("/epromak/epromak/dbwrite", "select", dataset, null);
		NodeList list = data.getElementsByTagName("papiery");
		List<EpromakOwnership> ownerships = new ArrayList<EpromakOwnership>();
		for (int i = 0, l = list.getLength(); i < l; i++) {
			Element e = (Element) list.item(i);
			try {
				EpromakPaper p = paperByCode.get(e.getAttribute("kodpapieru"));
				// if(p != null)
				ownerships.add(new EpromakOwnership(e, p));
				// else
				// Log.e(TAG, "invalid paper code: " + e.getAttribute("kodpapieru"));
			} catch (Exception ex) {
				Log.e(TAG, "can't create ownership", ex);
			}
		}
		return ownerships;
	}

	public List<EpromakOrderState> getOrderStates() throws GpwException, InvalidPasswordException {
		Document doc = conn.getBuilder().newDocument();
		Element dataset = doc.createElement("dataset");
		dataset.setAttribute("name", "pZlecBiez");
		Element e = doc.createElement("pZlecBiez");
		dataset.appendChild(e);
		for (int i = 0; i < EpromakOrderState.fields.length; i++)
			e.setAttribute(EpromakOrderState.fields[i], "");

		Document data = conn.sendXml("/epromak/epromak/dbwrite", "select", dataset, null);
		NodeList list = data.getElementsByTagName("dataset").item(0).getChildNodes();
		List<EpromakOrderState> orderStates = new ArrayList<EpromakOrderState>();
		for (int i = 0, l = list.getLength(); i < l; i++) {
			Element el = (Element) list.item(i);
			orderStates.add(new EpromakOrderState(el, paperByCode.get(el.getAttribute("kodpapieru"))));
		}
		return orderStates;
	}

	public EpromakOrderState getOrderStateById(String id) throws GpwException, InvalidPasswordException {
		for (EpromakOrderState o : getOrderStates())
			if (o.getId().equals(id))
				return o;
		return null;
	}

	public String submitOrder(EpromakOrder o) throws GpwException, InvalidPasswordException {
		roundOrder(o);
		Document doc = conn.getBuilder().newDocument();
		Element dataset = doc.createElement("dataset");
		dataset.setAttribute("name", "bEpmDyspGPW");
		Element e = doc.createElement("bEpmDyspGPW");
		dataset.appendChild(e);
		for (int i = 0; i < EpromakOrder.fields.length; i++) {
			String name = EpromakOrder.fields[i];
			if (o.get(name) != null)
				e.setAttribute(name, o.get(name));
			else
				e.setAttribute(name, "");
		}
		/*
		 * if(type == EmaklerType.millennium) { e.setAttribute("krdEmisja", "N"); if(o.get("dtWazTyp") == "J")
		 * e.setAttribute("dtWaz", o.get("dtSes")); }
		 */
		Document res = conn.sendXml("/epromak/epromak/dbwrite", "SendZlecGPW", dataset, null);
		Element dyspPrzy = (Element) conn.getElementByName(res, "dataset", "dyspPrzy").getFirstChild();
		if (dyspPrzy != null)
			return dyspPrzy.getAttribute("idDysp");
		else
			return null;
	}

	public String updateOrder(EpromakOrderState s, EpromakOrder o) throws GpwException, InvalidPasswordException {
		roundOrder(o);
		Document doc = conn.getBuilder().newDocument();
		Element dataset = doc.createElement("dataset");
		dataset.setAttribute("name", "bEpmDyspModyf");
		Element e = doc.createElement("bEpmDyspModyf");
		dataset.appendChild(e);
		e.setAttribute("idZlec", s.get("idzlecenia"));
		e.setAttribute("idOper", s.get("idzlecenia"));
		e.setAttribute("kodPW", s.get("kodpapieru"));
		for (int i = 0; i < EpromakOrder.updateFields.length; i++) {
			String name = EpromakOrder.updateFields[i];
			if (o.get(name) != null)
				e.setAttribute(name, o.get(name));
		}
		e.appendChild(s.getDesc(doc));
		Document res = conn.sendXml("/epromak/epromak/dbwrite", "SendZlecModyf", dataset, null);
		Element dyspPrzy = (Element) conn.getElementByName(res, "dataset", "dyspPrzy").getFirstChild();
		if (dyspPrzy != null)
			return dyspPrzy.getAttribute("idDysp");
		else
			return null;
	}

	public String cancelOrder(EpromakOrderState s) throws GpwException, InvalidPasswordException {
		Document doc = conn.getBuilder().newDocument();
		Element dataset = doc.createElement("dataset");
		dataset.setAttribute("name", "bEpmDyspAnl");
		Element e = doc.createElement("bEpmDyspAnl");
		dataset.appendChild(e);
		e.setAttribute("idZlec", s.get("idzlecenia"));
		e.setAttribute("idOper", s.get("idzlecenia"));
		Element desc = doc.createElement("description");
		e.appendChild(desc);
		desc.setAttribute("kodpapieru", s.get("kodpapieru"));
		desc.setAttribute("oferta", s.get("oferta"));
		desc.setAttribute("ilosc", s.get("ilosczlc"));
		desc.setAttribute("limitCeny", s.get("limitceny"));
		desc.setAttribute("data1Sesji", s.get("data1sesji"));
		desc.setAttribute("dataWaznosci", s.get("datawaznosci"));
		Document res = conn.sendXml("/epromak/epromak/dbwrite", "SendAnlZlec", dataset, null);
		Element dyspPrzy = (Element) conn.getElementByName(res, "dataset", "dyspPrzy").getFirstChild();
		if (dyspPrzy != null)
			return dyspPrzy.getAttribute("idDysp");
		else
			return null;
	}

	private void roundOrder(EpromakOrder o) throws GpwException, InvalidPasswordException {
		if (roundRules == null) {
			Document doc = conn.getBuilder().newDocument();
			Element dataset = doc.createElement("dataset");
			dataset.setAttribute("name", "sDokladnoscLimitu");
			dataset.setAttribute("schema", "");
			Element dokladnosc = doc.createElement("sDokladnoscLimitu");
			dokladnosc.setAttribute("gielda", "");
			dokladnosc.setAttribute("grupanotow", "");
			dokladnosc.setAttribute("rynekzlecen", "");
			dokladnosc.setAttribute("cenamin", "");
			dokladnosc.setAttribute("cenamax", "");
			dokladnosc.setAttribute("dokladnosc", "");
			dataset.appendChild(dokladnosc);
			Document data = conn.sendXml("/epromak/epromak/dbwrite", "select", dataset, null);

			Element resultDs = (Element) data.getElementsByTagName("dataset").item(0);
			NodeList list = resultDs.getChildNodes();
			String schema = resultDs.getAttribute("schema");
			String[] fields = schema.split(";");

			roundRules = new ArrayList<EpromakRoundRule>();
			for (int i = 0, l = list.getLength(); i < l; i++) {
				Element e = (Element) list.item(i);
				roundRules.add(new EpromakRoundRule(e.getAttribute("v"), fields));
			}
		}
		EpromakRoundRule rule = null;
		for (EpromakRoundRule r : roundRules) {
			// Log.d(TAG, r.toString());
			if (r.match(o)) {
				// Log.d(TAG, "matches!");
				rule = r;
				break;
			}
		}
		if (rule != null)
			rule.roundOrder(o);
	}
}
