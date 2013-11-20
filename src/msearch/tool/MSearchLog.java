/*
 * MediathekView
 * Copyright (C) 2008 W. Xaver
 * W.Xaver[at]googlemail.com
 * http://zdfmediathk.sourceforge.net/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package msearch.tool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import msearch.daten.MSearchConfig;
import org.apache.commons.lang3.StringUtils;

public class MSearchLog {

    public static StringBuffer textSystem = new StringBuffer(10000);
    public static StringBuffer textProgramm = new StringBuffer(10000);
    public static StringBuffer textFehler = new StringBuffer();
    public static boolean playerMeldungenAus = false;
    public static final int LOG_FEHLER = MSearchListenerMediathekView.EREIGNIS_LOG_FEHLER;
    public static final int LOG_SYSTEM = MSearchListenerMediathekView.EREIGNIS_LOG_SYSTEM;
    public static final int LOG_PLAYER = MSearchListenerMediathekView.EREIGNIS_LOG_PLAYER;
    public static final int FEHLER_ART_PROG = 0;
    public static final String FEHLER_ART_PROG_TEXT = "   Prog: ";
    public static final int FEHLER_ART_GETURL = 1;
    public static final String FEHLER_ART_GETURL_TEXT = " GetUrl: ";
    public static final int FEHLER_ART_MREADER = 2;
    public static final String FEHLER_ART_MREADER_TEXT = "MReader: ";
    public static final int FEHLER_ART_FILME_SUCHEN = 3;
    public static final String FEHLER_ART_FILME_SUCHEN_TEXT = "  Filme: ";
    // private
    private static final int MAX_LAENGE_1 = 50000;
    private static final int MAX_LAENGE_2 = 30000;
    private static int zeilenNrSystem = 0;
    private static int zeilenNrProgramm = 0;
    private static int zeilenNrFehler = 0;
    private static LinkedList<Integer[]> fehlerListe = new LinkedList<>(); // [Art, Fehlernummer, Anzahl, Exception(0,1 für ja, nein)]
    private static boolean progress = false;
    private static String progressText = "";
    private static Date startZeit = new Date(System.currentTimeMillis());
    private static Date stopZeit = null;
    private static File logfile = null;

    public void resetFehlerListe() {
        fehlerListe.clear();
    }

    public static void setLogFile(File log) {
        logfile = log;
    }

    public static synchronized void versionsMeldungen(String classname) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        MSearchLog.systemMeldung("");
        MSearchLog.systemMeldung("");
        MSearchLog.systemMeldung("##################################################################################");
        MSearchLog.systemMeldung("##################################################################################");
        MSearchLog.systemMeldung("Programmstart: " + sdf.format(startZeit));
        MSearchLog.systemMeldung("##################################################################################");
        MSearchLog.systemMeldung("##################################################################################");
        long totalMem = Runtime.getRuntime().totalMemory();
        MSearchLog.systemMeldung("totalMemory: " + totalMem / (1024L * 1024L) + " MiB");
        long maxMem = Runtime.getRuntime().maxMemory();
        MSearchLog.systemMeldung("maxMemory: " + maxMem / (1024L * 1024L) + " MiB");
        long freeMem = Runtime.getRuntime().freeMemory();
        MSearchLog.systemMeldung("freeMemory: " + freeMem / (1024L * 1024L) + " MiB");
        MSearchLog.systemMeldung("##################################################################################");
        //Version
        MSearchLog.systemMeldung(Funktionen.getProgVersionString());
        MSearchLog.systemMeldung("Compiled: " + Funktionen.getCompileDate());
        MSearchLog.systemMeldung("##################################################################################");
        MSearchLog.systemMeldung("Java");
        MSearchLog.systemMeldung("Classname: " + classname);
        String[] java = Funktionen.getJavaVersion();
        for (String ja : java) {
            MSearchLog.systemMeldung(ja);
        }
        MSearchLog.systemMeldung("");
        MSearchLog.systemMeldung("");
    }

    public static synchronized void startMeldungen(String classname) {
        versionsMeldungen(classname);
        MSearchLog.systemMeldung("##################################################################################");
        MSearchLog.systemMeldung("Programmpfad: " + Funktionen.getPathJar());
        MSearchLog.systemMeldung("Filmliste: " + MSearchConfig.dateiFilmliste);
        MSearchLog.systemMeldung("Useragent: " + MSearchConfig.getUserAgent());
        MSearchLog.systemMeldung("##################################################################################");
        MSearchLog.systemMeldung("");
        MSearchLog.systemMeldung("");
        if (MSearchConfig.senderAllesLaden) {
            MSearchLog.systemMeldung("Laden:  alles");
        } else {
            MSearchLog.systemMeldung("Laden:  nur update");
        }
        if (MSearchConfig.updateFilmliste) {
            MSearchLog.systemMeldung("Filmliste:  nur updaten");
        } else {
            MSearchLog.systemMeldung("Filmliste:  neu erstellen");
        }
        MSearchLog.systemMeldung("Import (ersetzen):  " + MSearchConfig.importUrl__ersetzen);
        MSearchLog.systemMeldung("Import (anhängen):  " + MSearchConfig.importUrl__anhaengen);
        MSearchLog.systemMeldung("Export Filmliste (XML):  " + MSearchConfig.exportFilmlisteXml);
        MSearchLog.systemMeldung("Export Filmliste (Json):  " + MSearchConfig.exportFilmlisteJson);
        if (MSearchConfig.nurSenderLaden != null) {
            MSearchLog.systemMeldung("Nur Sender laden:  " + StringUtils.join(MSearchConfig.nurSenderLaden, ','));
        }
        MSearchLog.systemMeldung("##################################################################################");
    }

    public static synchronized void debugMeldung(String text) {
        if (MSearchConfig.debug) {
            debugmeldung(text);
        }
    }

    // Fehlermeldung mit Exceptions
    public static synchronized void fehlerMeldung(int fehlerNummer, int art, String klasse, Exception ex) {
        fehlermeldung_(fehlerNummer, art, klasse, ex, new String[]{});
    }

    public static synchronized void fehlerMeldung(int fehlerNummer, int art, String klasse, Exception ex, String text) {
        fehlermeldung_(fehlerNummer, art, klasse, ex, new String[]{text});
    }

    public static synchronized void fehlerMeldung(int fehlerNummer, int art, String klasse, Exception ex, String text[]) {
        fehlermeldung_(fehlerNummer, art, klasse, ex, text);
    }

    // Fehlermeldungen
    public static synchronized void fehlerMeldung(int fehlerNummer, int art, String klasse, String text) {
        fehlermeldung_(fehlerNummer, art, klasse, null, new String[]{text});
    }

    public static synchronized void fehlerMeldung(int fehlerNummer, int art, String klasse, String[] text) {
        fehlermeldung_(fehlerNummer, art, klasse, null, text);
    }

    public static synchronized void systemMeldung(String[] text) {
        systemmeldung(text);
    }

    public static synchronized void systemMeldung(String text) {
        systemmeldung(new String[]{text});
    }

    public static synchronized void playerMeldung(String text) {
        if (!playerMeldungenAus) {
            playermeldung(new String[]{text});
        }
    }

    public static synchronized void progress(String texte) {
        progress = true;
        texte += "\r";
        progressText = texte;
        System.out.print(texte);
    }

    public static synchronized void progressEnde() {
        progress = false;
        progressText = "";
        System.out.print("                                                                                           \r");
        System.out.println();
        System.out.println();
    }

    public static void printEndeMeldung() {
        systemMeldung("");
        systemMeldung("");
        systemMeldung("");
        systemMeldung("");
        systemMeldung("##################################################################################");
        if (fehlerListe.size() == 0) {
            systemMeldung(" Keine Fehler :)");
        } else {
            // Fehler ausgeben
            int i_1;
            int i_2;
            for (int i = 1; i < fehlerListe.size(); ++i) {
                for (int k = i; k > 0; --k) {
                    i_1 = fehlerListe.get(k - 1)[1];
                    i_2 = fehlerListe.get(k)[1];
                    // if (str1.compareToIgnoreCase(str2) > 0) {
                    if (i_1 < i_2) {
                        fehlerListe.add(k - 1, fehlerListe.remove(k));
                    } else {
                        break;
                    }
                }
            }
            Iterator<Integer[]> it = fehlerListe.iterator();
            while (it.hasNext()) {
                Integer[] integers = it.next();
                String z;
                switch (integers[0]) {
                    case FEHLER_ART_MREADER:
                        z = FEHLER_ART_MREADER_TEXT;
                        break;
                    case FEHLER_ART_FILME_SUCHEN:
                        z = FEHLER_ART_FILME_SUCHEN_TEXT;
                        break;
                    case FEHLER_ART_GETURL:
                        z = FEHLER_ART_GETURL_TEXT;
                        break;
                    case FEHLER_ART_PROG:
                        z = FEHLER_ART_PROG_TEXT;
                        break;
                    default:
                        z = "";
                }
                boolean ex = integers[3] == 1;
                String strEx;
                if (ex) {
                    strEx = "Ex! ";
                } else {
                    strEx = "    ";
                }
                if (integers[1] < 0) {
                    systemMeldung(strEx + z + " Fehlernummer: " + integers[1] + " Anzahl: " + integers[2]);
                } else {
                    systemMeldung(strEx + z + " Fehlernummer:  " + integers[1] + " Anzahl: " + integers[2]);
                }
            }
        }
        systemMeldung("##################################################################################");
        // Laufzeit ausgeben
        stopZeit = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        int minuten;
        try {
            minuten = Math.round((stopZeit.getTime() - startZeit.getTime()) / (1000 * 60));
        } catch (Exception ex) {
            minuten = -1;
        }
        systemMeldung("");
        systemMeldung("");
        systemMeldung("##################################################################################");
        systemMeldung("   --> Beginn: " + sdf.format(startZeit));
        systemMeldung("   --> Fertig: " + sdf.format(stopZeit));
        systemMeldung("   --> Dauer[Min]: " + (minuten == 0 ? "<1" : minuten));
        systemMeldung("##################################################################################");
        systemMeldung("");
        systemMeldung("   und Tschuess");
        systemMeldung("");
        systemMeldung("");
        systemMeldung("##################################################################################");
    }

    private static void addFehlerNummer(int nr, int art, boolean exception) {
        Iterator<Integer[]> it = fehlerListe.iterator();
        int ex = exception ? (ex = 1) : (ex = 2);
        while (it.hasNext()) {
            Integer[] i = it.next();
            if (i[1].intValue() == nr) {
                i[0] = art;
                i[2]++;
                i[3] = ex;
                return;
            }
        }
        // dann gibts die Nummer noch nicht
        fehlerListe.add(new Integer[]{new Integer(art), new Integer(nr), new Integer(1), new Integer(ex)});
    }

    private static void fehlermeldung_(int fehlerNummer, int art, String klasse, Exception ex, String[] texte) {
        addFehlerNummer(fehlerNummer, art, ex != null);
        if (ex != null || MSearchConfig.debug) {
            // Exceptions immer ausgeben
            if (progress) {
                // dann brauchen wir erst eine Leerzeite um die Progresszeile zu löschen
                System.out.print("                                                                                           \r");
            }
            final String FEHLER = "Fehler(" + MSearchConst.PROGRAMMNAME + "): ";
            String x, z;
            if (ex != null) {
                x = "!";
            } else {
                x = "=";
            }
            switch (art) {
                case FEHLER_ART_MREADER:
                    z = "  ==>";
                    break;
                case FEHLER_ART_FILME_SUCHEN:
                    z = "   >>";
                    break;
                case FEHLER_ART_GETURL:
                    z = "  ++>";
                    break;
                case FEHLER_ART_PROG:
                default:
                    z = "*";
            }
            System.out.println(x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x);
            try {
                String s = getStackTrace(ex);
                System.out.println(s);
                logFileSchreiben(new String[]{s});
            } catch (Exception nix) {
            }
            //System.out.println(x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x + x);
            System.out.println(z + " Fehlernr: " + fehlerNummer);
            if (ex != null) {
                System.out.println(z + " Exception: " + ex.getMessage());
            }
            System.out.println(z + " " + FEHLER + klasse);
            notifyMediathekListener(LOG_FEHLER, FEHLER + klasse);
            for (int i = 0; i < texte.length; ++i) {
                System.out.println(z + "           " + texte[i]);
                notifyMediathekListener(LOG_FEHLER, texte[i]);
            }
            System.out.println("");
            if (progress) {
                System.out.print(progressText);
            }
        }
        logFileSchreiben(texte);
    }

    private static void debugmeldung(String texte) {
        if (progress) {
            // dann brauchen wir erst eine Leerzeite um die Progresszeile zu löschen
            System.out.print("                                                                                           \r");
        }
        System.out.println("|||| " + texte);
        if (progress) {
            System.out.print(progressText);
        }
        logFileSchreiben(new String[]{texte});
    }

    private static void systemmeldung(String[] texte) {
        if (progress) {
            // dann brauchen wir erst eine Leerzeite um die Progresszeile zu löschen
            System.out.print("                                                                                           \r");
        }
        final String z = ". ";
        if (texte.length <= 1) {
            System.out.println(z + " " + texte[0]);
            notifyMediathekListener(LOG_SYSTEM, texte[0]);
        } else {
            String zeile = "---------------------------------------";
            String txt;
            System.out.println(z + zeile);
            notifyMediathekListener(LOG_SYSTEM, zeile);
            for (int i = 0; i < texte.length; ++i) {
                txt = "| " + texte[i];
                System.out.println(z + txt);
                if (i == 0) {
                    notifyMediathekListener(LOG_SYSTEM, texte[i]);
                } else {
                    notifyMediathekListener(LOG_SYSTEM, "    " + texte[i]);
                }
            }
            notifyMediathekListener(LOG_SYSTEM, " ");
            System.out.println(z + zeile);
        }
        if (progress) {
            System.out.print(progressText);
        }
        logFileSchreiben(texte);
    }

    private synchronized static void logFileSchreiben(String[] texte) {
        // ins Logfile eintragen
        if (logfile != null) {
            OutputStreamWriter writer = null;
            try {
                writer = new OutputStreamWriter(new FileOutputStream(logfile, true));
                for (int i = 0; i < texte.length; ++i) {
                    String s = texte[i];
                    if (s.equals("")) {
                        writer.write("\n"); // nur leere Zeile schrieben
                    } else {
                        writer.write(DatumZeit.getJetzt_ddMMyyyy_HHmm() + "     " + s);
                        writer.write("\n");
                    }
                }
                writer.close();
            } catch (Exception ex) {
                System.out.println("Fehler beim Logfile schreiben: " + ex.getMessage());
            } finally {
                try {
                    writer.close();
                } catch (Exception ex) {
                }
            }
        }

    }

    private static void playermeldung(String[] texte) {
        if (progress) {
            // dann brauchen wir erst eine Leerzeite um die Progresszeile zu löschen
            System.out.print("                                                                                           \r");
        }
        final String z = "  >>";
        System.out.println(z + " " + texte[0]);
        notifyMediathekListener(LOG_PLAYER, texte[0]);
        for (int i = 1; i < texte.length; ++i) {
            System.out.println(z + " " + texte[i]);
            notifyMediathekListener(LOG_PLAYER, texte[i]);
        }
        if (progress) {
            System.out.print(progressText);
        }
    }

    public static void clearText(int art) {
        if (art == LOG_FEHLER) {
            zeilenNrFehler = 0;
            textFehler.setLength(0);
        } else if (art == LOG_SYSTEM) {
            zeilenNrSystem = 0;
            textSystem.setLength(0);
        } else if (art == LOG_PLAYER) {
            zeilenNrProgramm = 0;
            textProgramm.setLength(0);
        }
        MSearchListenerMediathekView.notify(art, MSearchLog.class.getName());
    }

    private static void notifyMediathekListener(int art, String zeile) {
        if (art == LOG_FEHLER) {
            addText(textFehler, "[" + getNr(zeilenNrFehler++) + "]   " + zeile);
        } else if (art == LOG_SYSTEM) {
            addText(textSystem, "[" + getNr(zeilenNrSystem++) + "]   " + zeile);
        } else if (art == LOG_PLAYER) {
            addText(textProgramm, "[" + getNr(zeilenNrProgramm++) + "]   " + zeile);
        }
        MSearchListenerMediathekView.notify(art, MSearchLog.class.getName());
    }

    private static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }

    private static String getNr(int nr) {
        final int MAX_STELLEN = 5;
        final String FUELL_ZEICHEN = "0";
        String str = String.valueOf(nr);
        while (str.length() < MAX_STELLEN) {
            str = FUELL_ZEICHEN + str;
        }
        return str;
    }

    private static void addText(StringBuffer text, String texte) {
        cut(text);
        text.append(texte);
        text.append(System.getProperty("line.separator"));
    }

    private static void cut(StringBuffer buffer) {
        if (buffer.length() > MAX_LAENGE_1) {
            buffer.delete(0, MAX_LAENGE_2);
        }
    }
}
