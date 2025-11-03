package ru.maelnor.ozonbomgebot.bot.service;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.CategoryTextAnnotation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import ru.maelnor.ozonbomgebot.bot.model.PricePoint;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PriceChartBuilder {

    private final List<PricePoint> points;
    private final String title;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public PriceChartBuilder(List<PricePoint> points, String title) {
        this.points = points;
        this.title = title;
    }

    public File buildChart(int width, int height, String outputPath) throws IOException {
        // --- 1. Формируем набор данных ---
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<String, Long> dataMap = new LinkedHashMap<>();

        for (PricePoint p : points) {
            String formattedDate = dateFormat.format(new Date(p.createdAtMs()));
            dataset.addValue(p.price(), "Цена", formattedDate);
            dataMap.put(formattedDate, p.price());
        }

        // --- 2. Создаём график ---
        JFreeChart chart = ChartFactory.createLineChart(
                title,
                null,
                null,
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        // --- 3. Стилизация ---
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(245, 245, 245));
        plot.setRangeGridlinePaint(new Color(46, 46, 46));
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(new Color(46, 46, 46));
        plot.setOutlinePaint(Color.GRAY);

        // --- 4. Диапазон по Y ---
        long min = dataMap.values().stream().min(Long::compare).orElse(0L);
        long max = dataMap.values().stream().max(Long::compare).orElse(100L);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRange(false);
        rangeAxis.setAutoRangeIncludesZero(false);
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setNumberFormatOverride(new DecimalFormat("#,###"));

        long span = max - min;
        if (span <= 0) {
            // все значения одинаковые (или одна точка)
            // сделаем небольшую "коробку" вокруг цены
            long padding = Math.max(50, Math.round(max * 0.05)); // минимум 50, иначе совсем плоско
            long lower = Math.max(0, max - padding);
            long upper = max + padding;
            rangeAxis.setRange(lower, upper);
        } else {
            long margin = Math.round(span * 0.05);
            rangeAxis.setRange(min - margin, max + margin);
        }

        // --- 5. Линия ---
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(25, 118, 210)); // синий
        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultShapesFilled(true);
        renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));

        // --- 6. Оси ---
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("Ubuntu", Font.BOLD, 20));

        // --- 7. Подписи MIN / MAX ---
        String minKey = null, maxKey = null;
        for (Map.Entry<String, Long> e : dataMap.entrySet()) {
            if (e.getValue().equals(min)) minKey = e.getKey();
            if (e.getValue().equals(max)) maxKey = e.getKey();
        }

        // чтобы не делить на 0 при вычислении смещения
        double annOffset = (span <= 0) ? Math.max(10, max * 0.01) : span * 0.02;

        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setGroupingSeparator(' ');
        DecimalFormat human = new DecimalFormat("#,###", dfs);

        if (minKey != null) {
            CategoryTextAnnotation minAnn = new CategoryTextAnnotation(
                    "MIN: " + human.format(min),
                    minKey,
                    min - annOffset
            );
            minAnn.setFont(new Font("Ubuntu", Font.BOLD, 15));
            minAnn.setPaint(new Color(200, 0, 0));
            plot.addAnnotation(minAnn);
        }

        if (maxKey != null && !maxKey.equals(minKey)) { // если одна точка, не дублируем
            CategoryTextAnnotation maxAnn = new CategoryTextAnnotation(
                    "MAX: " + human.format(max),
                    maxKey,
                    max + annOffset
            );
            maxAnn.setFont(new Font("Ubuntu", Font.BOLD, 15));
            maxAnn.setPaint(new Color(0, 128, 0));
            plot.addAnnotation(maxAnn);
        }

        // --- 8. Сохранение ---
        File outputFile = new File(outputPath);
        ChartUtils.saveChartAsJPEG(outputFile, chart, width, height);
        return outputFile;
    }
}
