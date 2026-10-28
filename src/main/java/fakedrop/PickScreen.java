package fakedrop;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PickScreen extends net.minecraft.client.gui.screen.Screen {
    private TextFieldWidget search;
    private final List<ClickableWidget> buttons = new ArrayList<>();
    private ButtonWidget toggle;

    public PickScreen() { super(Text.literal("FakeDrop")); }

    @Override
    protected void init() {
        search = new TextFieldWidget(textRenderer, width / 2 - 100, 30, 200, 20, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search item (e.g. totem, crystal)"));
        search.setChangedListener(s -> rebuild());
        addDrawableChild(search);
        setInitialFocus(search);

        toggle = ButtonWidget.builder(label(), b -> {
            FakeDrop.enabled = !FakeDrop.enabled;
            b.setMessage(label());
        }).dimensions(width / 2 - 100, 6, 200, 20).build();
        addDrawableChild(toggle);
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear fake items"), b -> FakeDrop.clearAll())
                .dimensions(width / 2 + 105, 6, 100, 20).build());
        rebuild();
    }

    private Text label() { return Text.literal("Fake drops: " + (FakeDrop.enabled ? "ON" : "OFF")); }

    private void rebuild() {
        for (ClickableWidget w : buttons) remove(w);
        buttons.clear();
        String q = search.getText().toLowerCase().trim().replace(" ", "_");
        int y = 58, n = 0;
        for (Item i : Registries.ITEM) {
            if (i == Items.AIR) continue;
            if (!q.isEmpty() && !Registries.ITEM.getId(i).getPath().contains(q)
                    && !i.getName().getString().toLowerCase().replace(" ", "_").contains(q)) continue;
            if (n++ >= 10) break;
            ButtonWidget b = ButtonWidget.builder(i.getName(), btn -> {
                FakeDrop.setItem(new ItemStack(i));
                close();
            }).dimensions(width / 2 - 100, y, 200, 18).build();
            y += 20;
            buttons.add(b);
            addDrawableChild(b);
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Selected: ").append(FakeDrop.item.getName()), width / 2, height - 20, 0xFFFFFF);
    }
}
