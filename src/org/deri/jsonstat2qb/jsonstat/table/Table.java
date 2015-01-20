package org.deri.jsonstat2qb.jsonstat.table;

import net.hamnaberg.funclite.CollectionOps;
import net.hamnaberg.funclite.Optional;
import org.deri.jsonstat2qb.jsonstat.Category;
import org.deri.jsonstat2qb.jsonstat.Data;
import org.deri.jsonstat2qb.jsonstat.Dataset;
import org.deri.jsonstat2qb.jsonstat.Dimension;
import org.deri.jsonstat2qb.jsonstat.util.IntCartesianProduct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.hamnaberg.funclite.Optional.some;
import static org.deri.jsonstat2qb.jsonstat.util.CollectionUtils.join;
import static org.deri.jsonstat2qb.jsonstat.util.CollectionUtils.product;

public final class Table {
    private Optional<String> title;
    private final List<TableHeader> headers = new ArrayList<>();
    private final List<List<Data>> rows = new ArrayList<>();

    public Table(Optional<String> title, List<TableHeader> headers, List<List<Data>> rows) {
        this.title = title;
        this.headers.addAll(headers);
        for (List<Data> row : rows) {
            this.rows.add(CollectionOps.newArrayList(row));
        }
    }

    public static Table fromDataset(Dataset dataset) {
        return fromDataset(dataset, findRowDimension(dataset));
    }

    public static Table fromDataset(Dataset dataset, Dimension rowDimension) {
        List<Dimension> dimensions = dataset.getDimensions();

        List<TableHeader> headers = buildHeader(dimensions, rowDimension.getId());
        List<List<Data>> rows = dataset.getRows(rowDimension);
        //TODO: maybe this should really be part of dataset.getRows()...
        int i = 0;
        for (String s : rowDimension.getCategory()) {
            List<Data> row = rows.get(i);
            int j = 0;
            row.add(j, new Data(rowDimension.getCategory().getLabel(s).getOrElse(s), Optional.<String>none()));
            for (Dimension dimension : dimensions) {
                if (dimension.isConstant()) {
                    boolean added = false;
                    for (String id : dimension.getCategory()) {
                        row.add(j, new Data(dimension.getCategory().getLabel(id).getOrElse(id), Optional.<String>none()));
                        added = true;
                    }
                    if (!added) {
                        row.add(j, new Data(dimension.getLabel().getOrElse(dimension.getId()), Optional.<String>none()));
                    }
                    j++;
                }
            }
            i++;
        }

        return new Table(dataset.getLabel(), headers, rows);
    }

    private static List<TableHeader> buildHeader(List<Dimension> dimensions, String rowDimension) {
        //TODO: This is stupid. Fix it.
        List<List<String>> categories = new ArrayList<>();
        List<TableHeader> headers = new ArrayList<>();
        for (Dimension dimension : dimensions) {
            boolean isRow = rowDimension.equals(dimension.getId());
            if (dimension.isRequired() && !isRow) {
                Category category = dimension.getCategory();
                List<String> cats = new ArrayList<>();
                for (String id : category) {
                    cats.add(category.getLabel(id).getOrElse(id));
                }
                categories.add(cats);
            }
            else if (dimension.isConstant()) {
                headers.add(new TableHeader(CollectionOps.headOption(dimension.getCategory()), dimension.getLabel()));
            }
            if (isRow) {
                headers.add(new TableHeader(Optional.<String>none(), dimension.getLabel()));
            }
        }

        List<String[]> combinations = product(categories);

        for (String[] combination : combinations) {
            String label = join(Arrays.asList(combination), " ");
            headers.add(new TableHeader(Optional.<String>none(), some(label)));
        }

        return headers;
    }


    private static Dimension findRowDimension(Dataset ds) {
        IntCartesianProduct p = ds.asCartasianProduct();
        return ds.getDimensions().get(p.getMaxIndex());
    }


    public Optional<String> getTitle() {
        return title;
    }

    public TableHeader getHeader(int index) {
        return headers.get(index);
    }

    public TableHeader getHeader(String id) {
        return getHeader(getHeaderIndex(id));
    }

    public int getHeaderIndex(String id) {
        for (int i = 0; i < headers.size(); i++) {
            TableHeader h = headers.get(i);
            if (h.getId().equals(some(id))) {
                return i;
            }
        }
        return -1;
    }

    public List<TableHeader> getHeaders() {
        return headers;
    }

    public List<Data> getRow(int index) {
        if (index < rows.size()) {
            return rows.get(index);
        }
        return Collections.emptyList();
    }

    public List<List<Data>> getRows() {
        return rows;
    }

    public <A> A render(Renderer<A> renderer) {
        return renderer.render(this);
    }
}
