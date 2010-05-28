/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.tor.tribes.types;

import de.tor.tribes.io.DataHolder;
import de.tor.tribes.io.UnitHolder;
import de.tor.tribes.util.support.SOSFormater;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Torridity
 */
public class SOSRequest {

    private Tribe mDefender = null;
    private Hashtable<Village, TargetInformation> attacks = null;

    public SOSRequest() {
        attacks = new Hashtable<Village, TargetInformation>();
    }

    public SOSRequest(Tribe pDefender) {
        setDefender(pDefender);
        attacks = new Hashtable<Village, TargetInformation>();
    }

    public void setDefender(Tribe pDefender) {
        mDefender = pDefender;
    }

    public Tribe getDefender() {
        return mDefender;
    }

    public void addTarget(Village pTarget) {
        TargetInformation targetInfo = attacks.get(pTarget);
        if (targetInfo == null) {
            targetInfo = new TargetInformation();
            attacks.put(pTarget, targetInfo);
        }
    }

    public TargetInformation getTargetInformation(Village pTarget) {
        return attacks.get(pTarget);
    }

    public Enumeration<Village> getTargets() {
        return attacks.keys();
    }

    public String toBBCode() {
        StringBuffer buffer = new StringBuffer();
        Enumeration<Village> targets = getTargets();
        while (targets.hasMoreElements()) {
            Village target = targets.nextElement();
            TargetInformation targetInfo = getTargetInformation(target);
            buffer.append(SOSFormater.format(target, targetInfo));
            buffer.append("\n\n");
        }
        return buffer.toString();
    }

    @Override
    public String toString() {
        String result = "Verteidiger: " + getDefender() + "\n";
        Enumeration<Village> targets = getTargets();

        while (targets.hasMoreElements()) {
            Village target = targets.nextElement();
            result += " Ziel: " + target + "\n";
            result += getTargetInformation(target);
            //result += "\n";
        }

        return result;
    }

    public class TargetInformation {

        private List<TimedAttack> attacks = null;
        private int iWallLevel = 20;
        private Hashtable<UnitHolder, Integer> troops = null;

        public TargetInformation() {
            attacks = new LinkedList<TimedAttack>();
            troops = new Hashtable<UnitHolder, Integer>();
        }

        /**
         * @return the attacks
         */
        public List<TimedAttack> getAttacks() {
            return attacks;
        }

        /**
         * @param attacks the attacks to set
         */
        public void addAttack(Village pSource, Date pArrive) {
            attacks.add(new TimedAttack(pSource, pArrive));
        }

        /**
         * @return the iWallLevel
         */
        public int getWallLevel() {
            return iWallLevel;
        }

        /**
         * @param iWallLevel the iWallLevel to set
         */
        public void setWallLevel(int iWallLevel) {
            this.iWallLevel = iWallLevel;
        }

        /**
         * @return the troops
         */
        public Hashtable<UnitHolder, Integer> getTroops() {
            return troops;
        }

        /**
         * @param troops the troops to set
         */
        public void addTroopInformation(UnitHolder pUnit, Integer pAmount) {
            troops.put(pUnit, pAmount);
        }

        public String getTroopInformationAsHTML() {
            StringBuffer b = new StringBuffer();

            for (UnitHolder unit : DataHolder.getSingleton().getUnits()) {
                Integer amount = troops.get(unit);
                if (amount != null) {
                    b.append("<img src=\"" + SOSRequest.class.getResource("/res/ui/" + unit.getPlainName() + ".png") + "\"/>&nbsp;" + amount + "\n");
                }
            }

            return b.toString();
        }

        @Override
        public String toString() {
            String result = " Stufe des Walls: " + getWallLevel() + "\n";
            Enumeration<UnitHolder> units = troops.keys();
            if (troops.isEmpty()) {
                result += " Truppen im Dorf: -Keine Informationen-\n\n";
            } else {
                result += " Truppen im Dorf:\n";
                while (units.hasMoreElements()) {
                    UnitHolder unit = units.nextElement();
                    result += "  " + troops.get(unit) + " " + unit + "\n";
                }
            }
            result += "\n";
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy HH:mm:ss.SSS");
            for (TimedAttack attack : attacks) {
                result += " * " + attack.getSource() + "(" + format.format(new Date(attack.getlArriveTime())) + ")\n";
            }
            result += "\n";
            return result;
        }
    }

    public class TimedAttack {

        private Village mSource = null;
        private long lArriveTime = 0;

        public TimedAttack(Village pSource, Date pArriveTime) {
            mSource = pSource;
            lArriveTime = pArriveTime.getTime();
        }

        /**
         * @return the mSource
         */
        public Village getSource() {
            return mSource;
        }

        /**
         * @param mSource the mSource to set
         */
        public void setSource(Village mSource) {
            this.mSource = mSource;
        }

        /**
         * @return the lArriveTime
         */
        public long getlArriveTime() {
            return lArriveTime;
        }

        /**
         * @param lArriveTime the lArriveTime to set
         */
        public void setlArriveTime(long lArriveTime) {
            this.lArriveTime = lArriveTime;
        }
    }
}
