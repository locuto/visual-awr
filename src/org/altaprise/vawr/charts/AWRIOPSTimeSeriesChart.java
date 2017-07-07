package org.altaprise.vawr.charts;

import java.awt.BorderLayout;
import java.awt.Color;

import java.util.Date;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import javax.swing.JPanel;
import org.altaprise.vawr.awrdata.AWRData;
import org.altaprise.vawr.awrdata.AWRMetrics;

import org.altaprise.vawr.awrdata.AWRRecord;

import org.altaprise.vawr.utils.SessionMetaData;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Minute;
import org.jfree.data.time.MovingAverage;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class AWRIOPSTimeSeriesChart extends RootChartFrame {

    private BorderLayout borderLayout = new BorderLayout();

    public AWRIOPSTimeSeriesChart() {

    }

    public AWRIOPSTimeSeriesChart(String metricName, String chartHeaderText) {
        super("Visual AWR Charting", chartHeaderText);

        this.setLayout(borderLayout);
        this.setSize(new java.awt.Dimension(800, 800));

        int totalNumRacInstances = 0;

        totalNumRacInstances = AWRData.getInstance().getNumRACInstances();

        for (int i = 0; i < totalNumRacInstances; i++) {

            int racInstNum = i + 1;

            String racInstNumS = Integer.toString(racInstNum);

            TimeSeriesCollection xyDataset = createDataset(racInstNumS, metricName);

            JFreeChart chart = createChart(xyDataset, metricName, racInstNum, "", "");

            ChartPanel chartPanel = (ChartPanel) createChartPanel(chart);

            THE_ROOT_CONTENT_PANEL.add(chartPanel);
        }
        TimeSeriesCollection xyDataset = AWRTimeSeriesChart.createMetricSumDataset(metricName);

        JFreeChart chart = createChart(xyDataset, metricName, 0, metricName + " - Sum All Nodes", "");
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getRenderer(0).setSeriesPaint(0, Color.YELLOW);

        ChartPanel chartPanel = (ChartPanel) createChartPanel(chart);

        THE_ROOT_CONTENT_PANEL.add(chartPanel);

        //Size of Y should be 200 (for header) + 260 * numCharts)
        THE_ROOT_CONTENT_PANEL.setPreferredSize(new java.awt.Dimension(600, 200 + (260*(totalNumRacInstances+1))));
        add(THE_SCROLL_PANE, BorderLayout.CENTER);

        this.setVisible(true);
    }

    public JPanel getChartPanel() {
        return THE_ROOT_CONTENT_PANEL;    
    }
    
    protected TimeSeriesCollection createDataset(String racInst, String awrMetric) {

        String metricLabel = AWRMetrics.getInstance().getMetricDescription(awrMetric);
        TimeSeriesCollection xyDataset = new TimeSeriesCollection();
        TimeSeries s1 = new TimeSeries(metricLabel);

        LinkedHashSet<String> snapshotIds = AWRData.getInstance().getUniqueSnapshotIds();
        Iterator iter = snapshotIds.iterator();
        while (iter.hasNext()) {
            String snapshotId = (String) iter.next();
            String metricValS = "0.0";
            Date snapshotDate = null;

            try {
                AWRRecord awrRec = AWRData.getInstance().getAWRRecordByKey(snapshotId, racInst);

                if (awrRec != null) {
                    metricValS = awrRec.getVal(awrMetric.toUpperCase());
                    snapshotDate = awrRec.getSnapShotDateTime();
                } else {
                    //How do we get the snapshot Id?
                    snapshotDate = this.getMissingSnapshotDate(snapshotId);
                    if (snapshotDate == null) {
                        throw new Exception("Could not find the snapshot date to plot.");
                    }
                }

                if (SessionMetaData.getInstance().debugOn()) {
                    System.out.println("TRACE: Adding date/SnapId/InstId/val: " + snapshotDate.toString() + "/" +
                                       snapshotId + "/" + racInst + "/" + metricValS + "/" + awrRec);
                }

                double metricValD = Double.parseDouble(metricValS);
                s1.add(new Minute(snapshotDate), metricValD);

            } catch (org.jfree.data.general.SeriesException se) {
                System.out.println("Error plotting SnapShot: " + snapshotId + ", Inst: " + racInst + " " +
                                   snapshotDate.toString());
                System.out.println(se.getLocalizedMessage());
                if (SessionMetaData.getInstance().debugOn()) {
                    //se.printStackTrace();
                }

            } catch (Exception e) {
                System.out.println("Error plotting SnapShot: " + snapshotId + ", Inst: " + racInst + " " +
                                   snapshotDate.toString());
                e.printStackTrace();
            }
        }

        xyDataset.addSeries(s1);

        final TimeSeries mav =
            MovingAverage.createMovingAverage(s1, "Moving Average", s1.getItemCount(), s1.getItemCount());

        xyDataset.addSeries(mav);

        return xyDataset;
    }

}