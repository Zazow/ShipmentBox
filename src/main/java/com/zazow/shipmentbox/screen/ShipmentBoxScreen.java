package com.zazow.shipmentbox.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zazow.shipmentbox.SBMod;
import com.zazow.shipmentbox.container.ShipmentBoxContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class ShipmentBoxScreen extends AbstractContainerScreen<ShipmentBoxContainer> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft",
            "textures/gui/container/shulker_box.png");

    // private ExtendedButton beanButton;

    public ShipmentBoxScreen(ShipmentBoxContainer container, Inventory playerInv, Component title) {
        super(container, playerInv, title);
        this.leftPos = 0;
        this.topPos = 0;
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        super.render(stack, mouseX, mouseY, partialTicks);
        this.font.draw(stack, this.title, this.leftPos + 20, this.topPos + 5, 0x404040);
        this.font.draw(stack, this.playerInventoryTitle, this.leftPos + 8, this.topPos + 75, 0x404040);
        this.renderTooltip(stack, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
//        this.beanButton = addRenderableWidget(
//                new ExtendedButton(this.leftPos, this.topPos, 16, 16, new TextComponent("beans"),
//                        btn -> Minecraft.getInstance().player.displayClientMessage(new TextComponent("beans"), false)));
    }

    @Override
    protected void renderBg(PoseStack stack, float mouseX, int mouseY, int partialTicks) {
        renderBackground(stack);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(stack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(PoseStack stack, int mouseX, int mouseY) {
    }
}
