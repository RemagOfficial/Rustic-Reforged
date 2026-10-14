package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12.2's {@code BlockFaceShape}, which every "can this thing hang from that block?" question in the
 * decor set answered.
 *
 * <p>Forge 1.12.2 let a block declare, per face, whether the face was {@code SOLID}, a thin
 * {@code CENTER}/{@code CENTER_BIG}/{@code CENTER_SMALL} stub, a {@code BOWL} or {@code UNDEFINED}, and
 * Rustic read that value directly: the chains asked for {@code SOLID || CENTER || CENTER_BIG ||
 * CENTER_SMALL} before hanging, the candles asked for {@code isSideSolid} (so {@code SOLID} only) before
 * sticking to a wall, and the candles, chains, lanterns, gargoyles and chandeliers all answered the
 * question themselves for their own faces.
 *
 * <p>1.21 deleted the declaration. The same information is now <em>derived</em> from the block's
 * support shape: {@link SupportType#RIGID} is the close relative of {@code SOLID} and
 * {@link SupportType#CENTER} of the three {@code CENTER_*} sizes, which vanilla's own wall torch and
 * lantern now test with. Derivation reproduces vanilla blocks for free, but it cannot express two
 * things Rustic needs:
 * <ul>
 *   <li>the three {@code CENTER} sizes collapse into one, so a chain can now hang from anything a candle
 *       can stand on. The shapes themselves are unchanged, so this only widens a handful of edge cases
 *       (a chain beside a carpet, say);</li>
 *   <li>a block whose collision box is thin cannot present a {@code RIGID} side face, whatever it wants.
 *       The legacy chandelier did exactly that - an empty frame one quarter block high that still reported
 *       {@code isSideSolid == true} on its four sides.</li>
 * </ul>
 *
 * <p>Both are handled by letting a Rustic block declare its own faces through
 * {@link RusticFaceShapes.Declarable}, which this shim consults before falling back to derivation.
 */
public final class RusticFaceShapes {

    private RusticFaceShapes() {
    }

    /** The legacy face shapes, with {@code AUTO} (which resolved to the derived value anyway) dropped. */
    public enum FaceShape {
        UNDEFINED,
        SOLID,
        CENTER,
        CENTER_BIG,
        CENTER_SMALL,
        /**
         * 1.12.2's {@code BOWL}: the hollow top of a cauldron or, here, the frame of a chandelier. It is
         * never an acceptable support in Rustic's own tests, so it behaves as {@link #UNDEFINED} for the
         * purposes of {@link #canAttach}.
         */
        BOWL;

        /** The four values the legacy chain and candle accepted as "something to hang from". */
        public boolean canSupportAttached() {
            return this == SOLID || this == CENTER || this == CENTER_BIG || this == CENTER_SMALL;
        }
    }

    /**
     * Implemented by the Rustic blocks that need to declare a face shape the 1.21 shape derivation
     * cannot express. Every other block - vanilla's or Rustic's - is answered by
     * {@link #of(BlockState, BlockGetter, BlockPos, Direction)} instead.
     */
    public interface Declarable {
        FaceShape rusticFaceShape(BlockState state, Direction face);
    }

    /**
     * The answer for one face of one block: the block's own declaration if it makes one, otherwise the
     * value the support shape implies.
     *
     * <p>The order of the two derivation tests matters. {@code RIGID} is asked first because it is the
     * stricter of the two and the mapping has to be one-valued: a full cube is both rigid and centred,
     * and legacy called that {@code SOLID}.
     */
    public static FaceShape of(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        if (state.getBlock() instanceof Declarable declarable) {
            return declarable.rusticFaceShape(state, face);
        }
        // An empty getter or position is only ever asked for by a test, and a block cannot answer the
        // question for a spot it has never seen, so fall back to the shape of the default state.
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

    /** 1.12.2's {@code faceShape == SOLID || CENTER || CENTER_BIG || CENTER_SMALL} chain of tests. */
    public static boolean canAttach(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return of(state, level, pos, face).canSupportAttached();
    }

    /** 1.12.2's {@code world.isSideSolid(pos, face, false)}, which meant "this face is SOLID". */
    public static boolean isSolidFace(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return of(state, level, pos, face) == FaceShape.SOLID;
    }
}
