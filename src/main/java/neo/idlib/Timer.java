package neo.idlib;

import static neo.idlib.Timer.State.TS_STARTED;
import static neo.idlib.Timer.State.TS_STOPPED;

import neo.idlib.Lib.idException;
import neo.idlib.Lib.idLib;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrList;

/**
 *
 */
public class Timer {

    static enum State {

        TS_STARTED,
        TS_STOPPED
    }

    /*
     ===============================================================================

     Clock tick counter. Should only be used for profiling.

     ===============================================================================
     */
    public static class idTimer {

        private State state;
        private static double base = -1;
        private double start;
        private double clockTicks;
        //
        //

        public idTimer() {
            this.state = TS_STOPPED;
            this.clockTicks = 0.0;
        }

        public idTimer(double _clockTicks) {
            this.state = TS_STOPPED;
            this.clockTicks = _clockTicks;
        }
//public					~idTimer( void );
//

        public idTimer oPlus(final idTimer t) {
            assert ((this.state == TS_STOPPED) && (t.state == TS_STOPPED));
            return new idTimer(this.clockTicks + t.clockTicks);
        }

        public idTimer oMinus(final idTimer t) {
            assert ((this.state == TS_STOPPED) && (t.state == TS_STOPPED));
            return new idTimer(this.clockTicks - t.clockTicks);
        }

        public idTimer oPluSet(final idTimer t) {
            assert ((this.state == TS_STOPPED) && (t.state == TS_STOPPED));
            this.clockTicks += t.clockTicks;
            return this;
        }

        public idTimer oMinSet(final idTimer t) {
            assert ((this.state == TS_STOPPED) && (t.state == TS_STOPPED));
            this.clockTicks -= t.clockTicks;
            return this;
        }

        public void Start() {
            assert (this.state == TS_STOPPED);
            this.state = TS_STARTED;
            this.start = idLib.sys.GetClockTicks();
        }

        public void Stop() {
            assert (this.state == TS_STARTED);
            this.clockTicks += idLib.sys.GetClockTicks() - this.start;
            if (base < 0.0) {
                InitBaseClockTicks();
            }
            if (this.clockTicks > base) {
                this.clockTicks -= base;
            }
            this.state = TS_STOPPED;
        }

        public void Clear() {
            this.clockTicks = 0.0;
        }

        public double ClockTicks() {
            assert (this.state == TS_STOPPED);
            return this.clockTicks;
        }

        public double Milliseconds() {
            assert (this.state == TS_STOPPED);
            return this.clockTicks / (idLib.sys.ClockTicksPerSecond() * 0.001);
        }

        private void InitBaseClockTicks() {
            final idTimer timer = new idTimer();
            double ct, b;
            int i;

            base = 0.0;
            b = -1.0;
            for (i = 0; i < 1000; i++) {
                timer.Clear();
                timer.Start();
                timer.Stop();
                ct = timer.ClockTicks();
                if ((b < 0.0) || (ct < b)) {
                    b = ct;
                }
            }
            base = b;
        }
    }

    /*
     ===============================================================================

     Report of multiple named timers.

     ===============================================================================
     */
    class idTimerReport {

        private idList<idTimer> timers;
        private idStrList names;
        private idStr reportName;
        //
        //

        public idTimerReport() {
        }
//public					~idTimerReport( void );
//

        public void SetReportName(final String name) {
            this.reportName = new idStr((name != null) ? name : "Timer Report");
        }

        public int AddReport(final String name) {
            if (name != null) {
                this.names.Append(new idStr(name));
                return this.timers.Append(new idTimer());
            }
            return -1;
        }

        public void Clear() {
            this.timers.DeleteContents(true);
            this.names.Clear();
            this.reportName.Clear();
        }

        public void Reset() {
            assert (this.timers.Num() == this.names.Num());
            for (int i = 0; i < this.timers.Num(); i++) {
                this.timers.oGet(i).Clear();
            }
        }

        public void PrintReport() throws idException {
            assert (this.timers.Num() == this.names.Num());
            idLib.common.Printf("Timing Report for %s\n", this.reportName);
            idLib.common.Printf("-------------------------------\n");
            float total = 0.0f;
            for (int i = 0; i < this.names.Num(); i++) {
                idLib.common.Printf("%s consumed %5.2f seconds\n", this.names.oGet(i), this.timers.oGet(i).Milliseconds() * 0.001f);
                total += this.timers.oGet(i).Milliseconds();
            }
            idLib.common.Printf("Total time for report %s was %5.2f\n\n", this.reportName, total * 0.001f);//TODO:char[] OR string
        }

        public void AddTime(final String name, idTimer time) {
            assert (this.timers.Num() == this.names.Num());
            int i;
            for (i = 0; i < this.names.Num(); i++) {
                if (this.names.oGet(i).Icmp(name) == 0) {
                    this.timers.oPluSet(i, time);
                    break;
                }
            }
            if (i == this.names.Num()) {
                final int index = AddReport(name);
                if (index >= 0) {
                    this.timers.oGet(index).Clear();
                    this.timers.oPluSet(index, time);
                }
            }
        }
    }
}
