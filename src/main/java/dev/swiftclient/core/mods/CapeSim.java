package dev.swiftclient.core.mods;

public final class CapeSim {
   private final int n;
   private final float[] px;
   private final float[] py;
   private final float[] ppx;
   private final float[] ppy;
   private final float len = 1.0F;
   private final int numIterations = 3;
   private final float maxBend = 5.0F;
   public float gravity = 25.0F;

   public CapeSim(int parts) {
      this.n = Math.max(2, parts);
      this.px = new float[this.n];
      this.py = new float[this.n];
      this.ppx = new float[this.n];
      this.ppy = new float[this.n];

      for (int i = 0; i < this.n; i++) {
         this.py[i] = -i;
         this.ppy[i] = -i;
      }
   }

   public int size() {
      return this.n;
   }

   public float x(int i) {
      return this.px[i];
   }

   public float y(int i) {
      return this.py[i];
   }

   public float lerpX(int i, float d) {
      return this.ppx[i] + (this.px[i] - this.ppx[i]) * d;
   }

   public float lerpY(int i, float d) {
      return this.ppy[i] + (this.py[i] - this.ppy[i]) * d;
   }

   public void applyMovement(float mx, float my) {
      this.ppx[0] = this.px[0];
      this.ppy[0] = this.py[0];
      this.px[0] = this.px[0] + mx;
      this.py[0] = this.py[0] + my;
   }

   public void simulate() {
      float dt = 0.05F;
      float downY = -this.gravity * dt;

      for (int i = 1; i < this.n; i++) {
         float tx = this.px[i];
         float ty = this.py[i];
         this.py[i] = this.py[i] + downY;
         this.ppx[i] = tx;
         this.ppy[i] = ty;
      }

      for (int i = 1; i < this.n; i++) {
         if (this.px[i] - this.px[0] > 0.0F) {
            this.px[i] = this.px[0];
         }
      }

      for (int ix = this.n - 2; ix >= 1; ix--) {
         double angle = (
               Math.atan2(this.py[ix + 1] - this.py[ix], this.px[ix + 1] - this.px[ix])
                  - Math.atan2(this.py[ix - 1] - this.py[ix], this.px[ix - 1] - this.px[ix])
            )
            * 57.2958;
         if (angle > 360.0) {
            angle -= 360.0;
         }

         if (angle < -360.0) {
            angle += 360.0;
         }

         double abs = Math.abs(angle);
         if (abs < 175.0) {
            this.replace(ix, angle, 176.0);
         }

         if (abs > 185.0) {
            this.replace(ix, angle, 184.0);
         }
      }

      for (int it = 0; it < 3; it++) {
         for (int s = this.n - 2; s >= 0; s--) {
            float cx = (this.px[s] + this.px[s + 1]) / 2.0F;
            float cy = (this.py[s] + this.py[s + 1]) / 2.0F;
            float dx = this.px[s] - this.px[s + 1];
            float dy = this.py[s] - this.py[s + 1];
            float d = (float)Math.sqrt(dx * dx + dy * dy);
            if (d < 1.0E-5F) {
               d = 1.0E-5F;
            }

            dx /= d;
            dy /= d;
            if (s != 0) {
               this.px[s] = cx + dx * 1.0F / 2.0F;
               this.py[s] = cy + dy * 1.0F / 2.0F;
            }

            this.px[s + 1] = cx - dx * 1.0F / 2.0F;
            this.py[s + 1] = cy - dy * 1.0F / 2.0F;
         }
      }

      for (int s = 0; s < this.n - 1; s++) {
         float dxx = this.px[s] - this.px[s + 1];
         float dyx = this.py[s] - this.py[s + 1];
         float dxxx = (float)Math.sqrt(dxx * dxx + dyx * dyx);
         if (dxxx < 1.0E-5F) {
            dxxx = 1.0E-5F;
         }

         dxx /= dxxx;
         dyx /= dxxx;
         this.px[s + 1] = this.px[s] - dxx * 1.0F;
         this.py[s + 1] = this.py[s] - dyx * 1.0F;
      }
   }

   private void replace(int i, double angle, double target) {
      double theta = target / 57.2958;
      float x = this.px[i - 1] - this.px[i];
      float y = this.py[i - 1] - this.py[i];
      if (angle < 0.0) {
         theta *= -1.0;
      }

      double cs = Math.cos(theta);
      double sn = Math.sin(theta);
      this.px[i + 1] = (float)(x * cs - y * sn + this.px[i]);
      this.py[i + 1] = (float)(x * sn + y * cs + this.py[i]);
   }
}
