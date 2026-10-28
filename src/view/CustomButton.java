package view;

import view.components.AppButton;

/**
 * Legacy name kept for screens that still use it; looks like a primary AppButton.
 * Those screens attach their own hover sound, so built-in sounds are off here.
 */
public class CustomButton extends AppButton {
    public CustomButton(String text) {
        super(text, Variant.PRIMARY);
        setSounds(false);
    }
}
