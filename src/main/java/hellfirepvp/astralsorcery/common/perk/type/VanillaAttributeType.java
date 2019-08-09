/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2019
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.perk.type;

import hellfirepvp.astralsorcery.common.data.research.ResearchHelper;
import hellfirepvp.astralsorcery.common.perk.PerkAttributeHelper;
import hellfirepvp.astralsorcery.common.perk.type.vanilla.VanillaPerkAttributeType;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;

import java.util.UUID;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: VanillaAttributeType
 * Created by HellFirePvP
 * Date: 09.08.2019 / 08:04
 */
public abstract class VanillaAttributeType extends PerkAttributeType implements VanillaPerkAttributeType {

    public VanillaAttributeType(ResourceLocation key) {
        super(key);
    }

    @Override
    public void onApply(PlayerEntity player, Dist side) {
        super.onApply(player, side);

        refreshAttribute(player);
    }

    @Override
    public void onRemove(PlayerEntity player, Dist side, boolean removedCompletely) {
        super.onRemove(player, side, removedCompletely);

        refreshAttribute(player);
    }

    @Override
    public void onModeApply(PlayerEntity player, ModifierType mode, Dist side) {
        super.onModeApply(player, mode, side);

        IAttributeInstance attr = player.getAttributes().getAttributeInstance(getAttribute());

        //The attributes don't get written/read from bytebuffer on local connection, but ARE in dedicated connections.
        //Remove minecraft's dummy instances in case we're on a dedicated server.
        if (side == Dist.CLIENT) {
            AttributeModifier modifier;
            if ((modifier = attr.getModifier(getID(mode))) != null) {
                if (!(modifier instanceof DynamicAttributeModifier)) {
                    attr.removeModifier(getID(mode));
                } else {
                    return;
                }
            }
        }

        switch (mode) {
            case ADDITION:
                attr.applyModifier(new DynamicAttributeModifier(getID(mode), getDescription() + " Add", this, mode, player, side));
                break;
            case ADDED_MULTIPLY:
                attr.applyModifier(new DynamicAttributeModifier(getID(mode), getDescription() + " Multiply Add", this, mode, player, side));
                break;
            case STACKING_MULTIPLY:
                attr.applyModifier(new DynamicAttributeModifier(getID(mode), getDescription() + " Stack Add", this, mode, player, side));
                break;
            default:
                break;
        }
    }

    @Override
    public void onModeRemove(PlayerEntity player, ModifierType mode, Dist side, boolean removedCompletely) {
        super.onModeRemove(player, mode, side, removedCompletely);

        IAttributeInstance attr = player.getAttributes().getAttributeInstance(getAttribute());
        switch (mode) {
            case ADDITION:
                attr.removeModifier(getID(mode));
                break;
            case ADDED_MULTIPLY:
                attr.removeModifier(getID(mode));
                break;
            case STACKING_MULTIPLY:
                attr.removeModifier(getID(mode));
                break;
            default:
                break;
        }
    }

    public void refreshAttribute(PlayerEntity player) {
        IAttributeInstance attr = player.getAttributes().getAttributeInstance(getAttribute());
        double base = attr.getBaseValue();
        if (base == 0) {
            attr.setBaseValue(1);
        } else {
            attr.setBaseValue(0);
        }
        attr.setBaseValue(base);
    }

    public abstract UUID getID(ModifierType mode);

    public abstract String getDescription();

    public abstract IAttribute getAttribute();

    static class DynamicAttributeModifier extends AttributeModifier {

        private PlayerEntity player;
        private Dist side;
        private PerkAttributeType type;

        public DynamicAttributeModifier(UUID idIn, String nameIn, PerkAttributeType type, ModifierType mode, PlayerEntity player, Dist side) {
            this(idIn, nameIn, type, mode.getVanillaAttributeOperation(), player, side);
        }

        public DynamicAttributeModifier(UUID idIn, String nameIn, PerkAttributeType type, Operation operationIn, PlayerEntity player, Dist side) {
            super(idIn, nameIn, 0, operationIn);
            this.setSaved(false);
            this.player = player;
            this.side = side;
            this.type = type;
        }

        @Override
        public double getAmount() {
            ModifierType mode = ModifierType.fromVanillaAttributeOperation(getOperation());
            return PerkAttributeHelper.getOrCreateMap(player, side)
                    .getModifier(player, ResearchHelper.getProgress(player, side), type, mode) - 1;
        }

    }

}