package io.github.zom.world.noise;

/**
 * OpenSimplex2 — 2D noise (S variant, smooth).
 * Vendored from github.com/KdotJPG/OpenSimplex2 (public domain / MIT).
 * Only the noise2() method is used by ProceduralMapGenerator.
 */
public final class OpenSimplex2 {

    private static final long PRIME_X = 0x5205402B9270C86FL;
    private static final long PRIME_Y = 0x598CD327003817B5L;
    private static final long HASH_MULTIPLIER = 0x53A3F72DEEC546F5L;
    private static final long SEED_FLIP_3D = -0x52D547B2E96ED629L;

    private static final double ROOT2OVER2 = 0.7071067811865476;
    private static final double SKEW_2D    = 0.366025403784439;
    private static final double UNSKEW_2D  = -0.21132486540518713;

    private static final int N_GRADS_2D_EXPONENT = 7;
    private static final int N_GRADS_2D = 1 << N_GRADS_2D_EXPONENT;

    private static final double[] GRADIENTS_2D;

    static {
        double[] grad2 = {
            0.38268343236509,  0.923879532511287,
            0.923879532511287, 0.38268343236509,
            0.923879532511287,-0.38268343236509,
            0.38268343236509, -0.923879532511287,
            -0.38268343236509, -0.923879532511287,
            -0.923879532511287,-0.38268343236509,
            -0.923879532511287, 0.38268343236509,
            -0.38268343236509,  0.923879532511287,
            0.130526192220052, 0.99144486137381,
            0.608761429008721, 0.793353340291235,
            0.793353340291235, 0.608761429008721,
            0.99144486137381,  0.130526192220052,
            0.99144486137381, -0.130526192220052,
            0.793353340291235,-0.608761429008721,
            0.608761429008721,-0.793353340291235,
            0.130526192220052,-0.99144486137381,
            -0.130526192220052,-0.99144486137381,
            -0.608761429008721,-0.793353340291235,
            -0.793353340291235,-0.608761429008721,
            -0.99144486137381, -0.130526192220052,
            -0.99144486137381,  0.130526192220052,
            -0.793353340291235, 0.608761429008721,
            -0.608761429008721, 0.793353340291235,
            -0.130526192220052, 0.99144486137381,
        };
        GRADIENTS_2D = new double[N_GRADS_2D * 2];
        for (int i = 0; i < grad2.length; i++)
            GRADIENTS_2D[i] = grad2[i] / 0.05481866623333;
        for (int i = grad2.length; i < GRADIENTS_2D.length; i++)
            GRADIENTS_2D[i] = GRADIENTS_2D[i % grad2.length];
    }

    private OpenSimplex2() {}

    /** Returns a noise value in approximately [-1, 1] for the given seed and coordinates. */
    public static double noise2(long seed, double x, double y) {
        double s  = SKEW_2D * (x + y);
        double xs = x + s, ys = y + s;
        return noise2UnskewedBase(seed, xs, ys);
    }

    private static double noise2UnskewedBase(long seed, double xs, double ys) {
        long xsb = fastFloor(xs), ysb = fastFloor(ys);
        double xi  = xs - xsb, yi  = ys - ysb;
        long  xsvp = xsb * PRIME_X, ysvp = ysb * PRIME_Y;
        boolean  inLowerHalf = (xi + yi < 1);
        double ax0 = inLowerHalf ? xi : xi - 1;
        double ay0 = inLowerHalf ? yi : yi - 1;
        long   hash = (inLowerHalf ? seed : seed ^ SEED_FLIP_3D)
            ^ (xsvp + ysvp);
        double a0  = 2.0/3 - ax0*ax0 - ay0*ay0;
        double value = 0;
        if (a0 > 0) {
            double a0s = a0*a0;
            value = a0s*a0s * grad2(hash, ax0, ay0);
        }
        // Second vertex
        double ax1 = ax0 + (inLowerHalf ? -1 : 1) * (1 + UNSKEW_2D*2);
        double ay1 = ay0 + (inLowerHalf ? -1 : 1) * (1 + UNSKEW_2D*2);
        double a1  = 2.0/3 - ax1*ax1 - ay1*ay1;
        if (a1 > 0) {
            double a1s = a1*a1;
            value += a1s*a1s * grad2(hash ^ PRIME_X ^ PRIME_Y, ax1, ay1);
        }
        // Third vertex
        if (inLowerHalf) {
            double x2 = xi - UNSKEW_2D*2 - 1;
            double y2 = yi - UNSKEW_2D*2;
            double a2  = 2.0/3 - x2*x2 - y2*y2;
            if (a2 > 0) {
                double a2s = a2*a2;
                value += a2s*a2s * grad2((seed ^ SEED_FLIP_3D) ^ (xsvp + PRIME_X + ysvp), x2, y2);
            }
        } else {
            double x2 = xi - UNSKEW_2D*2;
            double y2 = yi - UNSKEW_2D*2 - 1;
            double a2  = 2.0/3 - x2*x2 - y2*y2;
            if (a2 > 0) {
                double a2s = a2*a2;
                value += a2s*a2s * grad2((seed ^ SEED_FLIP_3D) ^ (xsvp + ysvp + PRIME_Y), x2, y2);
            }
        }
        return value;
    }

    private static double grad2(long hash, double dx, double dy) {
        int idx = (int)(hash >> (64 - N_GRADS_2D_EXPONENT)) & (N_GRADS_2D - 1);
        return GRADIENTS_2D[idx*2]*dx + GRADIENTS_2D[idx*2+1]*dy;
    }

    private static long fastFloor(double x) {
        long xi = (long)x;
        return x < xi ? xi - 1 : xi;
    }
}
