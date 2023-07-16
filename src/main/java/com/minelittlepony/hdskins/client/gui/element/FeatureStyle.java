package com.minelittlepony.hdskins.client.gui.element;

import java.util.Optional;

import com.minelittlepony.common.client.gui.Tooltip;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.style.Style;
import com.minelittlepony.hdskins.client.gui.GuiSkins;

import net.minecraft.text.Text;

public class FeatureStyle extends Style {

    private final Button element;

    private Optional<Tooltip> disabledTooltip = Optional.of(Tooltip.of(GuiSkins.HD_SKINS_OPTION_DISABLED_DESC));

    private boolean locked;

    public FeatureStyle(Button element) {
        this.element = element;
    }

    public FeatureStyle setLocked(boolean locked) {
        this.locked = locked;
        element.active &= !locked;

        return this;
    }

    @Override
    public Optional<Tooltip> getTooltip() {
        if (locked) {
            return disabledTooltip;
        }
        return super.getTooltip();
    }

    @Override
    public Style setTooltip(Tooltip tooltip) {
        disabledTooltip = Optional.of(Tooltip.of(
                Text.translatable("hdskins.warning.disabled.title",
                        tooltip.getString(),
                        GuiSkins.HD_SKINS_OPTION_DISABLED_DESC
                )
        ));
        return super.setTooltip(tooltip);
    }
}