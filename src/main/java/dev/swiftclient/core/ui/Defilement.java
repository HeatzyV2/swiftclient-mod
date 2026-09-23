package dev.swiftclient.core.ui;

public final class Defilement {
   private static final double VITESSE = 18.0;
   private static final double ARRIVE = 0.5;
   private static final double DT_MAX = 0.1;
   private double valeur;
   private double cible;
   private double max;
   private long derniere;

   public void cran(double amount, double pixelsParCran) {
      this.cible = this.borne(this.cible - amount * pixelsParCran);
   }

   public void pose(double valeurPx) {
      this.cible = this.borne(valeurPx);
      this.valeur = this.cible;
   }

   public void contenu(double hauteurContenu, double hauteurVisible) {
      this.max = Math.max(0.0, hauteurContenu - hauteurVisible);
      this.cible = this.borne(this.cible);
      this.valeur = this.borne(this.valeur);
   }

   public void anime() {
      long maintenant = System.nanoTime();
      double dt = this.derniere == 0L ? 0.016666666666666666 : (maintenant - this.derniere) / 1.0E9;
      this.derniere = maintenant;
      if (!(dt <= 0.0)) {
         if (dt > 0.1) {
            dt = 0.1;
         }

         this.valeur = this.valeur + (this.cible - this.valeur) * (1.0 - Math.exp(-18.0 * dt));
         if (Math.abs(this.cible - this.valeur) < 0.5) {
            this.valeur = this.cible;
         }
      }
   }

   public int px() {
      return (int)Math.round(this.valeur);
   }

   public int maxPx() {
      return (int)Math.round(this.max);
   }

   public void haut() {
      this.valeur = 0.0;
      this.cible = 0.0;
   }

   private double borne(double v) {
      return Math.max(0.0, Math.min(v, this.max));
   }
}
