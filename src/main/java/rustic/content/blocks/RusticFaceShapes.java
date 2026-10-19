package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;

public final class RusticFaceShapes {

    private RusticFaceShapes() {
    }

    public enum FaceShape {
        UNDEFINED,
        SOLID,
        CENTER,
        CENTER_BIG,
        CENTER_SMALL,

        BOWL;

        public boolean canSupportAttached() {
            return this == SOLID || this == CENTER || this == CENTER_BIG || this == CENTER_SMALL;
        }
    }

    public interface Declarable {
        FaceShape rusticFaceShape(BlockState state, Direction face);
    }

    public static FaceShape of(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        if (state.getBlock() instanceof Declarable declarable) {
            return declarable.rusticFaceShape(state, face);
        }

        if (level == null) {
            level = net.minecraft.world.level.EmptyBlockGetter.INSTANCE;
            pos = BlockPos.ZERO;
        }
        if (state.isFaceSturdy(level, pos, face, SupportType.RIGID)) {
            return FaceShape.SOLID;
        }
        if (state.isFaceSturdy(level, pos, face, SupportType.CENTER)) {
            return FaceShape.CENTER;
        }
        return FaceShape.UNDEFINED;
    }

    public static boolean canAttach(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return of(state, level, pos, face).canSupportAttached();
    }

    public static boolean isSolidFace(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return of(state, level, pos, face) == FaceShape.SOLID;
    }
}
