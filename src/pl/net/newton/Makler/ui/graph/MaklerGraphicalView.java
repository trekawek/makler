package pl.net.newton.Makler.ui.graph;

import java.math.BigDecimal;
import java.util.Date;
import org.achartengine.GraphicalView;
import org.achartengine.chart.AbstractChart;
import org.achartengine.chart.XYChart;
import org.achartengine.model.SeriesSelection;

import pl.net.newton.Makler.common.NumberFormatUtils;
import pl.net.newton.Makler.db.quote.Quote;
import pl.net.newton.Makler.history.EntryListWithIndexes;
import pl.net.newton.Makler.ui.graph.GraphView.GraphRange;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Implementacja GraphicalView z achartengine. Rysuję na widoku zmianę w procentach dla zadanego zakresu.
 * 
 * @author Igor Andruszkiewicz
 * 
 */
public class MaklerGraphicalView extends GraphicalView {
	private static final String TAG = "MaklerGraphicalView";

	private Paint paint = null;

	private final EntryListWithIndexes entries;

	private final Quote quote;

	private final int graphRange;

	private AbstractChart mChart;

	private double selectedValue;

	private PointF selectedPoint = null;

	public MaklerGraphicalView(Context context, AbstractChart arg1, final EntryListWithIndexes entries,
			final Quote quote, final int graphRange) {
		super(context, arg1);

		this.entries = entries;
		this.quote = quote;
		this.graphRange = graphRange;
		this.mChart = arg1;

		paint = new Paint();
		paint.setAntiAlias(true);
		// Must manually scale the desired text size to match screen density
		paint.setTextSize(13 * getResources().getDisplayMetrics().density);

	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		double change = 0;

		// For 1D graph range
		if (graphRange == GraphRange.D1) {
			if (quote.getKurs() != null && quote.getKursOdn() != null) {
				change = (quote.getKurs().doubleValue() - quote.getKursOdn().doubleValue())
						/ quote.getKursOdn().doubleValue() * 100;
			}
		} else {
			double x1 = ((double) entries.getClose(0)) / 100;
			double x2 = ((double) entries.getClose(entries.getLength() - 1)) / 100;

			Date d = new Date(entries.getDate(0));
			Log.d(TAG, "from: " + x1 + "@" + d);
			Date d2 = new Date(entries.getDate(entries.getLength() - 1));
			Log.d(TAG, "to: " + x2 + "@" + d2);

			change = (x2 - x1) / x1 * 100;
		}

		if (change < 0) {
			paint.setColor(Color.RED);
		} else if (change > 0) {
			paint.setColor(Color.GREEN);
		} else {
			paint.setColor(Color.GRAY);
		}

		String text = NumberFormatUtils.formatNumber(change) + "%";
		canvas.drawText(text, canvas.getWidth() - paint.measureText(text), 19, paint);

		/**
		 * Paint current selected value.
		 */
		BigDecimal kurs = quote.chooseKurs();
		paint.setColor(Color.GRAY);
		if (kurs != null) {
			double value = kurs.doubleValue();
			if (value > selectedValue) {
				paint.setColor(Color.RED);
			} else if (value < selectedValue) {
				paint.setColor(Color.GREEN);
			}
		}

		if (selectedPoint != null) {
			String valueToDraw = String.format("%.2f", selectedValue);

			canvas.drawText(valueToDraw, canvas.getWidth() - paint.measureText(valueToDraw), 50, paint);
			canvas.drawCircle(selectedPoint.x, selectedPoint.y, 4, paint);
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
			selectedValue = 0.0;
			selectedPoint = null;
			repaint();
			return false;
		}

		SeriesSelection seriesSelection = getCurrentSeriesAndPointForScreeXCoordinate(event.getX());

		if (seriesSelection == null && mChart instanceof XYChart) {
			Log.d(TAG, "No chart element was clicked");
			selectedValue = 0.0;
			selectedPoint = null;
		} else {
			double[] xy = new double[2];
			xy[0] = seriesSelection.getXValue();
			xy[1] = seriesSelection.getValue();
			xy = ((XYChart) mChart).toScreenPoint(xy);
			selectedPoint = new PointF((float) xy[0], (float) xy[1]);
			selectedValue = seriesSelection.getValue();

			repaint();
		}

		return false;
	}
}
