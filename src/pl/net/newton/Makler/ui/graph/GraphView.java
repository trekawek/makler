package pl.net.newton.Makler.ui.graph;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.chart.LineChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import pl.net.newton.Makler.R;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.db.symbol.Symbol;
import pl.net.newton.Makler.db.symbol.SymbolsDb;
import pl.net.newton.Makler.history.EntryListWithIndexes;
import pl.net.newton.Makler.history.HistoryFilter;
import pl.net.newton.Makler.history.service.HistoryService;
import pl.net.newton.Makler.ui.FullScreenGraph;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class GraphView extends LinearLayout implements OnGestureListener, OnTouchListener {

	public class GraphRange {
		// One day
		final static int D1 = 0;

		// Five day
		final static int D5 = 1;

		// One Month
		final static int M1 = 2;

		// Three months
		final static int M3 = 3;

		// One year
		final static int Y1 = 4;

		// Two years
		final static int Y2 = 5;

		// All
		final static int ALL = 6;
	};

	private int graphRange, newGraphRange;

	private int graphType = 0, newGraphType;

	private AlertDialog dialog;

	private Quote quote;

	private Handler mHandler;

	private HistoryService historyService;

	private SymbolsDb symbolsDb;

	private Context context;

	private Spinner rangeSpinner;

	/**
	 * add gesture support for GraphView
	 */
	private GestureDetector gestureDetector = null;

	private MaklerGraphicalView graphView = null;

	// private Paint paint;
	private boolean interActiveMode = false;

	private static final int GRAPH_SWIPE_MIN_DISTANCE = 60;

	private static final int GRAPH_SWIPE_THRESHOLD_VELOCITY = 200;

	private static final String TAG = "Makler";

	public GraphView(Context context, Quote quote, Handler mHandler, HistoryService historyService,
			SymbolsDb symbolsDb) {

		super(context);

		this.quote = quote;
		this.mHandler = mHandler;
		this.historyService = historyService;
		this.symbolsDb = symbolsDb;
		this.context = context;

		graphRange = GraphRange.D1;

		gestureDetector = new GestureDetector(context, this);

		setOnTouchListener(this);

		setFocusable(true);
		setFocusableInTouchMode(true);

		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		setOrientation(LinearLayout.VERTICAL);
		prepareDialog();
	}

	public void setGraphRange(int graphRange) {
		if (graphRange > GraphRange.ALL) {
			graphRange = GraphRange.D1;
		} else if (graphRange < GraphRange.D1) {
			graphRange = GraphRange.ALL;
		}
		this.graphRange = graphRange;
		mHandler.post(new Runnable() {
			public void run() {
				rangeSpinner.setSelection(GraphView.this.graphRange);
			}
		});
	}

	public void setGraphType(int graphType) {
		this.graphType = graphType;
	}

	public int getGraphRange() {
		return this.graphRange;
	}

	public int getGraphType() {
		return this.graphType;
	}

	public void refreshGraph(boolean force) {
		Symbol symbol = symbolsDb.getSymbolBySymbol(quote.getSymbol());

		try {
			if (historyService.isRangeExist(symbol, graphRange) == false) {
				showSpinner();
			}

			historyService.historyByInt(graphRange, symbol, force);
		} catch (NullPointerException e) {
			// TODO: handle exception
			// e.printStackTrace();
			Log.e(TAG, "npe during refreshing graph", e);
		}
	}

	public void gotEntries(final EntryListWithIndexes entries) {

		Thread t = new Thread(new Runnable() {
			public void run() {
				final LineChart xyChart = buildGraphView(entries);
				final BarChart volChart = buildVolGraphView(entries);

				mHandler.post(new Runnable() {
					public void run() {
						if (xyChart != null && volChart != null) {

							removeAllViews();

							graphView = new MaklerGraphicalView(context, xyChart, entries, quote, graphRange);
							GraphicalView graphViewVol = new GraphicalView(context, volChart);

							addView(graphView);
							addView(graphViewVol);

							LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) graphView
									.getLayoutParams();
							params.weight = 0.1f;
							graphView.setLayoutParams(params);

							params = (LinearLayout.LayoutParams) graphViewVol.getLayoutParams();
							params.weight = 0.9f;
							graphViewVol.setLayoutParams(params);
						} else {

							removeAllViews();

							TextView errorView = new TextView(context);
							errorView.setText(context.getString(R.string.graph_error_when_downloading));
							errorView.setGravity(Gravity.CENTER);
							errorView.setPadding(0, 20, 0, 0);

							addView(errorView);
						}
					}

				});
			}
		});
		t.start();
	}

	private BarChart buildVolGraphView(EntryListWithIndexes entries) {
		if (entries == null || entries.getLength() == 0) {
			return null;
		}

		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		XYSeries series = new XYSeries("Wolumen");
		for (int i = 0; i < entries.getLength(); i++) {
			series.add(entries.getGraphIndex(i), entries.getVol(i));
		}

		dataset.addSeries(series);

		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		renderer.setMargins(new int[] { 0, 0, 0, 0 });
		renderer.setAxesColor(Color.DKGRAY);
		renderer.setShowCustomTextGrid(false);
		renderer.setShowLabels(false);
		renderer.setShowGrid(false);
		renderer.setShowLegend(false);
		renderer.setShowAxes(true);
		renderer.setXLabels(0);
		renderer.setYLabels(6);

		if (graphRange == GraphRange.D1) {
			renderer.setXAxisMax(HistoryFilter.MINUTES_IN_DAY);
		}

		SimpleSeriesRenderer r = new SimpleSeriesRenderer();
		r.setColor(Color.rgb(0xCC, 0x33, 0x00));
		renderer.addSeriesRenderer(r);

		return new BarChart(dataset, renderer, Type.STACKED);
	}

	private LineChart buildGraphView(EntryListWithIndexes entries) {
		if (entries == null || entries.getLength() == 0)
			return null;

		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		XYSeries series = new XYSeries(quote.getSymbol());
		double min = -1, max = -1;
		for (int i = 0; i < entries.getLength(); i++) {
			double value = ((double) entries.getClose(i)) / 100;
			series.add(entries.getGraphIndex(i), value);

			if (min == -1 || min > value)
				min = value;
			if (max == -1 || max < value)
				max = value;
		}
		dataset.addSeries(series);

		boolean kursOdn = false;
		if (graphRange == GraphRange.D1 && quote != null && quote.getKursOdn() != null
				&& quote.getKursOdn().compareTo(BigDecimal.ZERO) > 0 && quote.getUpdate() != null
				&& quote.getUpdate().get(Calendar.HOUR_OF_DAY) >= 9) {

			double kursOdnDouble = quote.getKursOdn().doubleValue();
			series = new XYSeries("Kurs odn.");
			series.add(0, kursOdnDouble);
			series.add(HistoryFilter.MINUTES_IN_DAY, kursOdnDouble);
			dataset.addSeries(series);
			kursOdn = true;

			if (min > kursOdnDouble) {
				min = kursOdnDouble;
			}
			if (max < kursOdnDouble) {
				max = kursOdnDouble;
			}

		}

		XYMultipleSeriesRenderer renderer = getRenderer(kursOdn, min, max);

		renderer.setYAxisMax(max);
		renderer.setYAxisMin(min);

		if (graphRange == GraphRange.D1) {
			renderer.setXAxisMax(HistoryFilter.MINUTES_IN_DAY);
		} else {
			renderer.setXAxisMax(entries.getGraphIndex(entries.getLength() - 1));
		}
		renderer.setXAxisMin(entries.getGraphIndex(0));

		switch (graphRange) {
			case GraphRange.D1:
				addDailyLabels(renderer);
				renderer.setChartTitle(context.getString(R.string.d1));
				break;
			case GraphRange.D5:
				addLabels(5, entries, renderer, "dd.MM");
				renderer.setChartTitle(context.getString(R.string.d5));
				break;
			case GraphRange.M1:
				addLabels(6, entries, renderer, "dd.MM");
				renderer.setChartTitle(context.getString(R.string.m1));
				break;
			case GraphRange.M3:
				addLabels(7, entries, renderer, "dd.MM");
				renderer.setChartTitle(context.getString(R.string.m3));
				break;
			case GraphRange.Y1:
				addLabels(6, entries, renderer, "MM.yyyy");
				renderer.setChartTitle(context.getString(R.string.r1));
				break;
			case GraphRange.Y2:
				addLabels(6, entries, renderer, "MM.yyyy");
				renderer.setChartTitle(context.getString(R.string.r2));
				break;
			case GraphRange.ALL:
				addLabels(6, entries, renderer, "MM.yyyy");
				renderer.setChartTitle(context.getString(R.string.all));
				break;
		}

		return new LineChart(dataset, renderer);
	}

	private XYMultipleSeriesRenderer getRenderer(boolean kursOdn, double min, double max) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		if (graphRange == GraphRange.D1) {
			renderer.setXAxisMax(HistoryFilter.MINUTES_IN_DAY);
		}

		renderer.setAxisTitleTextSize(16);
		renderer.setChartTitleTextSize(20);
		renderer.setLabelsTextSize(15);
		renderer.setMargins(new int[] { 0, 0, 0, 0 });
		renderer.setAxesColor(Color.DKGRAY);
		renderer.setLabelsColor(Color.LTGRAY);
		renderer.setShowLabels(true);
		renderer.setShowCustomTextGrid(true);
		renderer.setXLabels(0);
		renderer.setXLabelsAlign(Align.LEFT);
		renderer.setYLabels(10);
		renderer.setYLabelsAlign(Align.LEFT);
		renderer.setShowGrid(true);
		renderer.setShowLegend(false);

		XYSeriesRenderer r = new XYSeriesRenderer();
		r.setColor(Color.rgb(0x00, 0x99, 0xCC));
		r.setLineWidth(2);
		renderer.addSeriesRenderer(r);

		if (kursOdn) {
			r = new XYSeriesRenderer();
			r.setColor(Color.rgb(0xFF, 0xAA, 0x56));
			r.setLineWidth(1);
			renderer.addSeriesRenderer(r);
		}

		renderer.setZoomEnabled(false);
		renderer.setZoomEnabled(false, false);
		renderer.setPanEnabled(false);
		renderer.setPanEnabled(false, false);
		renderer.setClickEnabled(false);

		return renderer;
	}

	private void addDailyLabels(XYMultipleSeriesRenderer renderer) {
		for (int i = 0; i <= 8; i++) {
			renderer.addXTextLabel(i * 60, String.format("%02d", 9 + i));
		}
	}

	private void addLabels(int labels, EntryListWithIndexes entries, XYMultipleSeriesRenderer renderer,
			String format) {
		int lastDay = -1;
		List<Calendar> days = new ArrayList<Calendar>();
		List<Integer> daysIndexes = new ArrayList<Integer>();
		for (int i = 0; i < entries.getLength(); i++) {
			Calendar d = Calendar.getInstance();
			d.setTimeInMillis(entries.getDate(i));
			int value = entries.getGraphIndex(i);
			if (d.get(Calendar.DATE) != lastDay) {
				lastDay = d.get(Calendar.DATE);
				days.add(d);
				daysIndexes.add(value);
			}
		}

		if (labels > days.size())
			labels = days.size();
		if (labels == 0)
			return;

		DateFormat sdf = new SimpleDateFormat(format, Locale.US);
		int d = days.size() / labels;
		for (int i = 0; i < labels; i++) {
			int j = i * d;
			renderer.addXTextLabel(daysIndexes.get(j), sdf.format(days.get(j)));
		}
	}

	private void showSpinner() {
		mHandler.post(new Runnable() {
			public void run() {
				RelativeLayout layout = new RelativeLayout(context);

				removeAllViews();

				addView(layout);

				LayoutParams params = (LayoutParams) layout.getLayoutParams();
				params.width = LayoutParams.MATCH_PARENT;
				params.height = LayoutParams.MATCH_PARENT;
				layout.setLayoutParams(params);

				ProgressBar spinner = new ProgressBar(context);
				layout.addView(spinner);
				spinner.setIndeterminate(true);
				RelativeLayout.LayoutParams spinnerParams = (android.widget.RelativeLayout.LayoutParams) spinner
						.getLayoutParams();
				spinnerParams.width = LayoutParams.WRAP_CONTENT;
				spinnerParams.height = LayoutParams.WRAP_CONTENT;
				spinnerParams.addRule(RelativeLayout.CENTER_IN_PARENT, -1);
				spinner.setLayoutParams(spinnerParams);
			}
		});
	}

	/**
	 * Przełącz wykres na następny zakres.
	 */
	public void switchToNextRage() {
		setGraphRange(graphRange + 1);

		refreshGraph(false);
	}

	/**
	 * Przełącz wykres na poprzedni zakres.
	 */
	public void switchToPreviousRage() {

		setGraphRange(graphRange - 1);

		refreshGraph(false);
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		boolean detected = false;

		if (Math.abs(velocityX) > GRAPH_SWIPE_THRESHOLD_VELOCITY) {
			if (e1.getX() - e2.getX() > GRAPH_SWIPE_MIN_DISTANCE) {
				switchToNextRage();
				detected = true;
			} else if (e2.getX() - e1.getX() > GRAPH_SWIPE_MIN_DISTANCE) {
				switchToPreviousRage();
				detected = true;
			}
		}

		return detected;
	}

	public void onLongPress(MotionEvent e) {
		if (!interActiveMode) {
			Toast.makeText(getContext(), context.getString(R.string.graph_interactive_mode_on),
					Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getContext(), context.getString(R.string.graph_interactive_mode_off),
					Toast.LENGTH_LONG).show();
		}

		interActiveMode = !interActiveMode;
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	public void onShowPress(MotionEvent e) {
	}

	public boolean onDown(MotionEvent e) {
		return true;
	}

	public boolean onSingleTapUp(MotionEvent e) {

		// don't allow to start full screen graph in FullScreenGraph activity
		if (context instanceof FullScreenGraph) {
			return false;
		}

		Intent intent = new Intent(context, FullScreenGraph.class);
		intent.putExtra("symbol", quote.getSymbol());
		intent.putExtra("graphRange", getGraphRange());
		intent.putExtra("graphType", getGraphType());
		context.startActivity(intent);

		return true;
	}

	public boolean onTouch(View v, MotionEvent event) {
		// If GraphView was already created then forward MontionEvents.
		if (graphView != null) {
			graphView.onTouchEvent(event);
		}

		return gestureDetector.onTouchEvent(event);
	}

	public void prepareDialog() {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.graph_dialog, null);

		rangeSpinner = (Spinner) layout.findViewById(R.id.graphRange);
		rangeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				newGraphRange = arg2;
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				newGraphRange = 0;
			}
		});

		dialog = new AlertDialog.Builder(context).setTitle("Wybierz zakres danych").setView(layout)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if (newGraphRange != graphRange || graphType != newGraphType) {
							graphRange = newGraphRange;
							graphType = newGraphType;
							refreshGraph(false);
						}
					}
				}).create();
	}

	public void changeGraphRange() {
		mHandler.post(new Runnable() {
			public void run() {
				dialog.show();
			}
		});
	}

}
