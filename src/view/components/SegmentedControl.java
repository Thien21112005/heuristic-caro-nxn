package view.components;

import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A row of mutually exclusive answers; the chosen one is circled in pen, as on a
 * paper quiz. Left and right arrows move the choice while the row has focus.
 */
public class SegmentedControl<T> extends JPanel {
    public static final int HEIGHT = 46;

    private final List<T> values = new ArrayList<>();
    private final List<AppButton> buttons = new ArrayList<>();
    private T selected;
    private Consumer<T> onChange = v -> {};

    public SegmentedControl() {
        super(new GridLayout(1, 0, 4, 0));
        setOpaque(false);
    }

    public SegmentedControl<T> add(T value, String label) {
        AppButton b = new AppButton(label, AppButton.Variant.PLAIN).compact();
        b.setFont(Theme.hand(Theme.BODY));
        b.addActionListener(e -> select(value, true));
        bindArrow(b, KeyEvent.VK_LEFT, -1);
        bindArrow(b, KeyEvent.VK_RIGHT, 1);
        values.add(value);
        buttons.add(b);
        add(b);
        return this;
    }

    private void bindArrow(AppButton b, int key, int step) {
        KeyStroke stroke = KeyStroke.getKeyStroke(key, 0);
        b.getInputMap(WHEN_FOCUSED).put(stroke, "move" + step);
        b.getActionMap().put("move" + step, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int next = Math.floorMod(values.indexOf(selected) + step, values.size());
                select(values.get(next), true);
                buttons.get(next).requestFocusInWindow();
            }
        });
    }

    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(text);
        for (AppButton b : buttons) b.setToolTipText(text);
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
            boolean chosen = values.get(i).equals(value);
            buttons.get(i).setVariant(chosen ? AppButton.Variant.CIRCLED : AppButton.Variant.PLAIN);
            buttons.get(i).getAccessibleContext().setAccessibleDescription(chosen ? "selected" : null);
        }
        if (notify) onChange.accept(value);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, HEIGHT);
    }
}
