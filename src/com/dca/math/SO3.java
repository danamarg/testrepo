public class SO3 {
    // Your Java implementation here
    // Operations for rigid rotations in Klampt.
    // All rotations are represented by a 9-list specifying the entries of the rotation matrix in column major form.
    // In other words, given a 3x3 matrix::
    // [a11,a12,a13]
    // [a21,a22,a23]
    // [a31,a32,a33],
    // Klamp't represents the matrix as a list [a11,a21,a31,a12,a22,a32,a13,a23,a33].
    // The reasons for this representation are 1) simplicity, and 2) a more convenient interface with C code.

    public static String str(double[] R) {
        // Converts a rotation to a string.
        StringBuilder result = new StringBuilder();
        for (double v : R) {
            result.append(v).append(" ");
        }
        return result.toString();
    }

    public static double[] identity() {
        // Returns the identity rotation.
        return new double[]{1., 0., 0., 0., 1., 0., 0., 0., 1.};
    }

    public static double[] inv(double[] R) {
        // Inverts the rotation.
        double[] Rinv = {R[0], R[3], R[6], R[1], R[4], R[7], R[2], R[5], R[8]};
        return Rinv;
    }

    public static double[] apply(double[] R, double[] point) {
        // Applies the rotation to a point.
        return new double[]{
                R[0] * point[0] + R[3] * point[1] + R[6] * point[2],
                R[1] * point[0] + R[4] * point[1] + R[7] * point[2],
                R[2] * point[0] + R[5] * point[1] + R[8] * point[2]
        };
    }

    public static double[][] matrix(double[] R) {
        // Returns the 3x3 rotation matrix corresponding to R.
        return new double[][]{
                {R[0], R[3], R[6]},
                {R[1], R[4], R[7]},
                {R[2], R[5], R[8]}
        };
    }

    public static double[] fromMatrix(double[][] mat) {
        // Returns a rotation R corresponding to the 3x3 rotation matrix mat.
        double[] R = new double[9];
        R[0] = mat[0][0];
        R[1] = mat[1][0];
        R[2] = mat[2][0];
        R[3] = mat[0][1];
        R[4] = mat[1][1];
        R[5] = mat[2][1];
        R[6] = mat[0][2];
        R[7] = mat[1][2];
        R[8] = mat[2][2];
        return R;
    }
        public static double[][] ndarray(double[] R) {
        // Returns the 3x3 numpy rotation matrix corresponding to R.
        return matrix(R);
    }

    public static double[] fromNdarray(double[][] mat) {
        // Returns a rotation R corresponding to the 3x3 rotation matrix mat.
        return fromMatrix(mat);
    }

    public static double[] mul(double[] R1, double[] R2) {
        // Multiplies two rotations.
        if (R1.length != 9) throw new IllegalArgumentException("R1 is not a rotation matrix");
        if (R2.length != 9) throw new IllegalArgumentException("R2 is not a rotation matrix (did you mean to use apply())?");
        double[][] m1 = matrix(R1);
        double[][] m2T = matrix(inv(R2));
        double[][] mres = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                mres[i][j] = VectorOps.dot(m1[i], m2T[j]);
            }
        }
        return fromMatrix(mres);
    }

    public static double trace(double[] R) {
        // Computes the trace of the rotation matrix.
        return R[0] + R[4] + R[8];
    }

    public static double angle(double[] R) {
        // Returns absolute deviation of R from identity
        double ctheta = (trace(R) - 1.0) * 0.5;
        return Math.acos(Math.max(Math.min(ctheta, 1.0), -1.0));
    }

       public static Vector3 rpy(Rotation R) {
        double[] result = new double[3];
        double[][] m = matrix(R);
        double _sb = Math.min(1.0, Math.max(m[2][0], -1.0));
        double b = -Math.asin(_sb); // m(2,0)=-sb
        double cb = Math.cos(b);
        if (Math.abs(cb) > 1e-7) {
            double ca = m[0][0] / cb; // m(0,0)=ca*cb
            ca = Math.min(1.0, Math.max(ca, -1.0));
            double a;
            if (Math.signum(m[1][0]) == Math.signum(cb)) // m(1,0)=sa*cb
                a = Math.acos(ca);
            else
                a = 2 * Math.PI - Math.acos(ca);

            double cc = m[2][2] / cb;  // m(2,2)=cb*cc
            cc = Math.min(1.0, Math.max(cc, -1.0));
            double c;
            if (Math.signum(m[2][1]) == Math.signum(cb)) // m(2,1)=cb*sc
                c = Math.acos(cc);
            else
                c = Math.PI * 2 - Math.acos(cc);
            result[0] = c;
            result[1] = b;
            result[2] = a;
        } else {
            double c = 0;
            double _sa = Math.min(1.0, Math.max(m[0][1], -1.0));
            double a = -Math.asin(_sa);
            if (Math.signum(Math.cos(a)) != Math.signum(m[1][1])) // m(1,1)=ca
                a = Math.PI - a;
            result[0] = c;
            result[1] = b;
            result[2] = a;
        }
        return new Vector3(result);
    }

    public static Rotation from_rpy(Vector3 rollpitchyaw) {
        double[] angles = rollpitchyaw.getElements();
        Rotation Rx = from_axis_angle(new Tuple(new double[]{1, 0, 0}, angles[0]));
        Rotation Ry = from_axis_angle(new Tuple(new double[]{0, 1, 0}, angles[1]));
        Rotation Rz = from_axis_angle(new Tuple(new double[]{0, 0, 1}, angles[2]));
        return mul(Rz, mul(Ry, Rx));
    }

        public static double[] rotationVector(double[] R) {
        double theta = angle(R);
        if (Math.abs(theta - Math.PI) < 0.5) {
            double c = Math.cos(theta);
            double x2 = (R[0] - c) / (1.0 - c);
            double y2 = (R[4] - c) / (1.0 - c);
            double z2 = (R[8] - c) / (1.0 - c);
            if (x2 < 0) {
                assert (x2 > -1e-5);
                x2 = 0;
            }
            if (y2 < 0) {
                assert (y2 > -1e-5);
                y2 = 0;
            }
            if (z2 < 0) {
                assert (z2 > -1e-5);
                z2 = 0;
            }
            double x = theta * Math.sqrt(x2);
            double y = theta * Math.sqrt(y2);
            double z = theta * Math.sqrt(z2);
            if (Math.abs(theta - Math.PI) < 1e-5) {
                double xy = R[3];
                double xz = R[6];
                double yz = R[7];
                if (x > y) {
                    if (x > z) {
                        if (xy < 0) y = -y;
                        if (xz < 0) z = -z;
                    } else {
                        if (yz < 0) y = -y;
                        if (xz < 0) x = -x;
                    }
                } else {
                    if (y > z) {
                        if (xy < 0) x = -x;
                        if (yz < 0) z = -z;
                    } else {
                        if (yz < 0) y = -y;
                        if (xz < 0) x = -x;
                    }
                }
            } else {
                double eps = theta - Math.PI;
                if (eps * (R[5] - R[7]) > 0) x = -x;
                if (eps * (R[6] - R[2]) > 0) y = -y;
                if (eps * (R[1] - R[3]) > 0) z = -z;
            }
            return new double[]{x, y, z};
        }
        double scale = 1;
        if (Math.abs(theta) > 1e-5) scale = theta / Math.sin(theta);
        return vectorOps.mul(deskew(R), scale);
    }

    public static double[] axisAngle(double[] R) {
        double[] m = rotationVector(R);
        return new double[]{unit(m), norm(m)};
    }

    public static double[] fromAxisAngle(double[] aa) {
        return rotation(aa[0], aa[1]);
    }

    public static double[][] fromRotationVector(double[] w) {
        double length = norm(w);
        if (length < 1e-7) return identity();
        double[] scaledW = vectorOpsMul(w, 1.0 / length);
        return rotation(scaledW, length);
    }

    public static double[] fromQuaternion(double[] q) {
        double w = q[0], x = q[1], y = q[2], z = q[3];
        double x2 = x + x, y2 = y + y, z2 = z + z;
        double xx = x * x2, xy = x * y2, xz = x * z2;
        double yy = y * y2, yz = y * z2, zz = z * z2;
        double wx = w * x2, wy = w * y2, wz = w * z2;

        double a11 = 1.0 - (yy + zz);
        double a12 = xy - wz;
        double a13 = xz + wy;
        double a21 = xy + wz;
        double a22 = 1.0 - (xx + zz);
        double a23 = yz - wx;
        double a31 = xz - wy;
        double a32 = yz + wx;
        double a33 = 1.0 - (xx + yy);

        return new double[]{a11, a21, a31, a12, a22, a32, a13, a23, a33};
    }

    public static double[] quaternion(double[] R) {
        double tr = trace(R) + 1.0;
        double a11 = R[0], a21 = R[1], a31 = R[2], a12 = R[3], a22 = R[4], a32 = R[5], a13 = R[6], a23 = R[7], a33 = R[8];

        if (tr > 1e-5) {
            double s = Math.sqrt(tr);
            double w = s * 0.5;
            s = 0.5 / s;
            double x = (a32 - a23) * s;
            double y = (a13 - a31) * s;
            double z = (a21 - a12) * s;
            return unit(new double[]{w, x, y, z});
        } else {
            int[] nxt = {1, 2, 0};
            int i = 0;
            if (a22 > a11) i = 1;
            if (a33 > Math.max(a11, a22)) i = 2;
            int j = nxt[i];
            int k = nxt[j];
            double[][] M = matrix(R);

            double[] quaternion = new double[4];
            double s = Math.sqrt((M[i][i] - (M[j][j] + M[k][k])) + 1.0);
            quaternion[i] = s * 0.5;

            if (Math.abs(s) < 1e-7) {
                throw new IllegalArgumentException("Could not solve for quaternion... Invalid rotation matrix?");
            } else {
                s = 0.5 / s;
                quaternion[3] = (M[k][j] - M[j][k]) * s;
                quaternion[j] = (M[i][j] + M[j][i]) * s;
                quaternion[k] = (M[i][k] + M[k][i]) * s;
            }
            return unit(new double[]{quaternion[3], quaternion[0], quaternion[1], quaternion[2]});
        }
    }

    public static double distance(double[] R1, double[] R2) {
        double[] R = mul(R1, inv(R2));
        return angle(R);
    }

    public static double error(double[] R1, double[] R2) {
        double[] R = mul(R1, inv(R2));
        return moment(R);
    }

    public static double[][] crossProduct(double[] w) {
        return new double[][]{
                {0, w[2], -w[1]},
                {-w[2], 0, w[0]},
                {w[1], -w[0], 0}
        };
    }

    public static double[] diag(double[] R) {
        return new double[]{R[0], R[4], R[8]};
    }

    public static double[] deskew(double[] R) {
        return new double[]{0.5 * (R[5] - R[7]), 0.5 * (R[6] - R[2]), 0.5 * (R[1] - R[3])};
    }

    public static double[] rotation(double[] axis, double angle) {
        double cm = Math.cos(angle);
        double sm = Math.sin(angle);

        double[] R = crossProduct(axis);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                R[i * 3 + j] += axis[i] * axis[j] * (1.0 - cm);
            }
        }
        R[0] += cm;
        R[4] += cm;
        R[8] += cm;
        return R;
    }

    public static double[] canonical(double[] v) {
        if (Math.abs(normSquared(v) - 1.0) > 1e-4) {
            throw new RuntimeException("Nonunit vector supplied to canonical()");
        }
        if (v.length != 3) {
            throw new IllegalArgumentException("Vector must be of length 3");
        }
        if (Math.abs(v[0] - 1.0) < 1e-5) {
            return identity();
        } else if (Math.abs(v[0] + 1.0) < 1e-5) {
            // flip of basis
            double[] R = identity();
            R[0] = -1.0;
            R[4] = -1.0;
            return R;
        } else {
            double[] R = Arrays.copyOf(v, 9);
            double x = v[0], y = v[1], z = v[2];
            double scale = 1.0 / (1.0 + x);
            R[3] = -y;
            R[4] = x + scale * z * z;
            R[5] = -scale * y * z;
            R[6] = -z;
            R[7] = -scale * y * z;
            R[8] = x + scale * y * y;
            return R;
        }
    }

    public static double[] align(double[] a, double[] b) {
        double an = norm(a);
        double bn = norm(b);
        if (Math.abs(an) < 1e-5 || Math.abs(bn) < 1e-5) {
            return identity();
        }
        a = div(a, an);
        b = div(b, bn);
        double[] v = cross(a, b);
        double c = dot(a, b);
        if (Math.abs(c + 1) < 1e-5) { // rotation of pi
            double[] temp = cross(a, new double[]{0, 0, 1});
            double vn = norm(temp);
            if (vn < 1e-5) {
                temp = cross(a, new double[]{0, 1, 0});
                vn = norm(temp);
            }
            return rotation(div(temp, vn), Math.PI);
        }
        double[] vhat = crossProduct(v);
        double[] vhat2 = mul(vhat, vhat);
        return add(identity(), madd(vhat, vhat2, 1.0 / (1.0 + c)));
    }

    public static double[] interpolate(double[] R1, double[] R2, double u) {
        double[] R = mul(inv(R1), R2);
        double[] m = moment(R);
        double angle = norm(m);
        if (angle == 0) return R1;
        double[] axis = div(m, angle);
        return mul(R1, rotation(axis, angle * u));
    }

    public static Function<Double, double[]> interpolator(double[] R1, double[] R2) {
        double[] R = mul(inv(R1), R2);
        double[] m = moment(R);
        double angle = norm(m);
        double[] axis;
        if (angle == 0) {
            axis = new double[]{1, 0, 0};
        } else {
            axis = div(m, angle);
        }
        return u -> mul(R1, rotation(axis, angle * u));
    }

    public static double det(double[] R) {
        double[][] m = matrix(R);
        return m[0][0] * m[1][1] * m[2][2] + m[0][1] * m[1][2] * m[2][0] + m[0][2] * m[1][0] * m[2][1] -
                m[0][0] * m[1][2] * m[2][1] - m[0][1] * m[1][0] * m[2][2] - m[0][2] * m[1][1] * m[2][0];
    }

    public static boolean isRotation(double[] R, double tol) {
        double[] RRt = mul(R, inv(R));
        double[] err = sub(RRt, identity());
        for (double v : err) {
            if (Math.abs(v) > tol) {
                return false;
            }
        }
        return det(R) >= 0;
    }

    public static double[] sample() {
        Random random = new Random();
        double[] q = new double[]{random.nextGaussian(), random.nextGaussian(), random.nextGaussian(), random.nextGaussian()};
        q = div(q, norm(q));
        double theta = Math.acos(q[3]) * 2.0;
        double[] m;
        if (Math.abs(theta) < 1e-8) {
            m = new double[]{0, 0, 0};
        } else {
            m = mul(unit(Arrays.copyOfRange(q, 0, 3)), theta);
        }
        return fromMoment(m);
    }