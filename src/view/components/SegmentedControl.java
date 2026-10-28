package view.components;

import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A row of mutually exclusive buttons; the selected one is highlighted.
 */
public class SegmentedControl<T> extends JPanel {
    private final List<T> values = new ArrayList<>();
    private final List<AppButton> buttons = new ArrayList<>();
    private T selected;
    private Consumer<T> onChange = v -> {};

    public SegmentedControl() {
        super(new GridLayout(1, 0, 8, 0));
        setOpaque(false);
    }

    public SegmentedControl<T> add(T value, String label) {
        AppButton b = new AppButton(label, AppButton.Variant.SECONDARY).compact();
        b.setFont(Theme.semibold(15f));
        b.addActionListener(e -> select(value, true));
        values.add(value);
        buttons.add(b);
        add(b);
        return this;
    }

    public void setOnChange(Consumer<T> onChange) {
        this.onChange = onChange;
    }

    public T getSelected() { return selected; }

    public void setSelected(T value) {
        select(value, false);
    }

    private void select(T value, boolean notify) {
        selected = value;
        for (int i = 0; i < values.size(); i++) {
            buttons.get(i).setVariant(values.get(i).equals(value) ? AppButton.Variant.PRIMARY : AppButton.Variant.SECONDARY);
        }
        if (notify) onChange.accept(value);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, 44);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 44);
    }
}
