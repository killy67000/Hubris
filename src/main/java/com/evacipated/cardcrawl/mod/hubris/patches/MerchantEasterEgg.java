package com.evacipated.cardcrawl.mod.hubris.patches;

import basemod.abstracts.CustomPlayer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.Skeleton;
import com.evacipated.cardcrawl.mod.hubris.relics.NiceRug;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.SmilingMask;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MerchantEasterEgg
{
    @SpirePatch(
            clz=AbstractPlayer.class,
            method=SpirePatch.CLASS
    )
    public static class IsMerchantField
    {
        public static SpireField<Boolean> isMerchant = new SpireField<>(() -> false);
    }

    @SpirePatch(
            clz=AbstractPlayer.class,
            method="render"
    )
    public static class Render
    {
        public static void Prefix(AbstractPlayer __instance, SpriteBatch sb)
        {
            if (__instance.hasRelic(NiceRug.ID) && __instance.hasRelic(SmilingMask.ID)) {
                if (!IsMerchantField.isMerchant.get(__instance)) {
                    IsMerchantField.isMerchant.set(__instance, true);
                    try {
                        Field flipHorizontal = AbstractCreature.class.getDeclaredField("flipHorizontal");
                        flipHorizontal.setAccessible(true);
                        Method loadAnimation = AbstractCreature.class.getDeclaredMethod("loadAnimation", String.class, String.class, float.class);
                        loadAnimation.setAccessible(true);

                        loadAnimation.invoke(__instance, "images/npcs/merchant/skeleton.atlas", "images/npcs/merchant/skeleton.json", 1.0F);
                        flipHorizontal.set(__instance, true);

                        __instance.hb_w = __instance.hb.width = 180.0f * Settings.scale;
                        __instance.hb_h = __instance.hb.height = 170.0f * Settings.scale;
                        __instance.hb_y -= 40.0f * Settings.scale;

                        AnimationState.TrackEntry e = __instance.state.setAnimation(0, "idle", true);
                        e.setTime(e.getEndTime() * MathUtils.random());
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @SpirePatch(
            clz=AbstractPlayer.class,
            method="renderPlayerImage"
    )
    @SpirePatch(
            clz=basemod.abstracts.CustomPlayer.class,
            method="renderPlayerImage"
    )
    public static class RenderPlayImage
    {
        public static SpireReturn<Void> Prefix(AbstractPlayer __instance, SpriteBatch sb)
        {
            if (__instance.hasRelic(NiceRug.ID) && __instance.hasRelic(SmilingMask.ID)) {
                if (__instance instanceof CustomPlayer) {
                    try {
                        Field f = AbstractCreature.class.getDeclaredField("skeleton");
                        f.setAccessible(true);
                        Skeleton skeleton = (Skeleton) f.get(__instance);

                        __instance.state.update(Gdx.graphics.getDeltaTime());
                        __instance.state.apply(skeleton);
                        skeleton.updateWorldTransform();
                        skeleton.setPosition(__instance.drawX + __instance.animX, __instance.drawY + __instance.animY + AbstractDungeon.sceneOffsetY);

                        skeleton.setColor(__instance.tint.color);
                        skeleton.setFlip(__instance.flipHorizontal, false);
                        sb.end();
                        CardCrawlGame.psb.begin();
                        AbstractPlayer.sr.draw(CardCrawlGame.psb, skeleton);
                        CardCrawlGame.psb.end();
                        sb.begin();
                    } catch (IllegalAccessException | NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    return SpireReturn.Return(null);
                }
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(
            clz=AbstractPlayer.class,
            method="render"
    )
    public static class UndoFlipAfterDeath
    {
        private static Field renderCorpse;

        static
        {
            try {
                renderCorpse = AbstractPlayer.class.getDeclaredField("renderCorpse");
                renderCorpse.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        public static void Prefix(AbstractPlayer __instance, SpriteBatch sb)
        {
            try {
                if (renderCorpse != null && (boolean)renderCorpse.get(__instance) && IsMerchantField.isMerchant.get(__instance)) {
                    Field flipHorizontal = AbstractCreature.class.getDeclaredField("flipHorizontal");
                    flipHorizontal.setAccessible(true);
                    flipHorizontal.set(__instance, false);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
