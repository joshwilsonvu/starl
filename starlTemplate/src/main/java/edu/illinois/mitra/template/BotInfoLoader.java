package edu.illinois.mitra.template;

import android.content.Context;
import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.illinois.mitra.starl.models.ModelRegistry;

/**
 * This class parses the XML files related to robot and device selection.
 *
 * The XML configuration files contain all info for specific hardware addresses.
 * Each tablet/phone and each robot is assigned a color for identification. All
 * addresses and names are based on the color.
 *
 * The XML files are located under PROJECT_ROOT/starlTemplate/src/main/java/res/xml.
 *
 * @see #loadBotInfo()
 */
public class BotInfoLoader {
    // to access resources
    private final Context context;
    // to store entries from bot_info.xml
    private final List<BotInfoXmlEntry> botInfoXmlEntries = new ArrayList<>();
    // color, modeltype, null -> MAC address
    private final Map<BotInfoXmlEntry, String> botAddresses = new HashMap<>();
    // color, null, devicetype -> IP address
    private final Map<BotInfoXmlEntry, String> deviceAddresses = new HashMap<>();
    // color -> name
    private final Map<String, String> botNames = new HashMap<>();

    /**
     * Constructs a new instance.
     * @param context an Android context, in order to access resources
     */
    BotInfoLoader(Context context) {

        this.context = context;
    }

    /**
     * Load the information of the robots to be used in the application, as described in
     * bot_info.xml.
     *
     * This method is rather expensive and should be called only once.
     *
     * @return a non-null array of BotInfo objects
     * @throws IllegalStateException if the XML files are not correct
     */
    public BotInfo[] loadBotInfo() {
        // load all bot and device pairs to use
        readXml(R.xml.bot_info, new ParserTask() {
            public void run(XmlPullParser parser) throws XmlPullParserException, IOException {

                BotInfoLoader.this.readInfo(parser);
            }
        });
        if (botInfoXmlEntries.isEmpty()) {
            return new BotInfo[0];
        }


        // load bot MAC addresses
        readXml(R.xml.bot_addresses, new ParserTask() {
            public void run(XmlPullParser parser) throws XmlPullParserException, IOException {
                BotInfoLoader.this.readMacAddresses(parser);
            }
        });

        // load device IP addresses
        readXml(R.xml.device_addresses, new ParserTask() {
            public void run(XmlPullParser parser) throws XmlPullParserException, IOException {
                BotInfoLoader.this.readIpAddresses(parser);
            }
        });

        // load bot names
        readXml(R.xml.bot_names, new ParserTask() {
            @Override
            public void run(XmlPullParser parser) throws XmlPullParserException, IOException {
                BotInfoLoader.this.readNames(parser);
            }
        });

        // assemble BotInfo entries
        BotInfo[] retVal = new BotInfo[botInfoXmlEntries.size()];
        for (int i = 0; i < botInfoXmlEntries.size(); i++) {
            BotInfoXmlEntry entry = botInfoXmlEntries.get(i);
            // lookup name by color
            String name = botNames.get(entry.color);
            // lookup MAC address by color and modelType
            String mac = botAddresses.get(new BotInfoXmlEntry(entry.color, entry.modelType, null));
            // lookup IP address by color and deviceType
            String ip = deviceAddresses.get(new BotInfoXmlEntry(entry.color, null, entry.deviceType));
            // null values if components not found
            retVal[i] = new BotInfo(name, entry.modelType, mac, entry.deviceType, ip);
        }

        return retVal;
    }

    /**
     * Process all inner tags, given an XML file with one enclosing outer tag and
     * a list of inner tags without nesting.
     * @param file the resource file id to load
     * @param task the task that, given a parser, will process each tag
     */
    private void readXml(int file, ParserTask task) {

        try (XmlResourceParser parser = context.getResources().getXml(file)) {
//            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

            while (parser.next() != XmlResourceParser.END_TAG) {
                if(parser.getEventType()==parser.START_TAG){
                    task.run(parser);
                }
                else {
                    parser.next();
                }

            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }

    // reads bot_info.xml to determine which robots and devices to use
    private void readInfo(XmlPullParser parser) throws IOException, XmlPullParserException {
        String color = null, modelType = null, deviceType = null;
        if (parser.getAttributeCount() >= 3) {
            color = parser.getAttributeValue(null, "color");
            modelType = parser.getAttributeValue(null, "modeltype");
            deviceType = parser.getAttributeValue(null, "devicetype");
        }

        if (color == null || modelType == null || deviceType == null) {
            throw new IllegalStateException("Bot info XML entries require color, modeltype, and " +
                    "devicetype attributes.");
        }

        if (!ModelRegistry.canCreate(modelType)) {
            throw new IllegalStateException(modelType + " is not a valid model type.");
        }

        parser.nextTag(); // exit self-closing tag
        botInfoXmlEntries.add(new BotInfoXmlEntry(color, modelType, deviceType));
    }

    // reads bot_addresses.xml to determine which robots are available and their MAC addresses
    private void readMacAddresses(XmlPullParser parser) throws IOException, XmlPullParserException {
        String color = null, modelType = null;
        if (parser.getAttributeCount() >= 2) {
            color = parser.getAttributeValue(null, "color");
            modelType = parser.getAttributeValue(null, "modeltype");
        }
        if (color == null || modelType == null) {
            throw new IllegalStateException("Bot address XML entries require color and modeltype attributes.");
        }
        if (parser.isEmptyElementTag()) {
            throw new IllegalStateException("Bot address XML entries should not be empty element tags.");
        }

        // record the MAC address
        while (parser.getEventType() != parser.END_TAG){
            if(parser.getEventType() == parser.TEXT){
                botAddresses.put(new BotInfoXmlEntry(color, modelType, null), parser.getText());
            }
            parser.next();
        }

    }

    // reads device_addresses.xml to determine which devices are available and their IP addresses
    private void readIpAddresses(XmlPullParser parser) throws IOException, XmlPullParserException {
        String color = null, devicetype = null;
        if (parser.getAttributeCount() >= 2) {
            color = parser.getAttributeValue(null, "color");
            devicetype = parser.getAttributeValue(null, "devicetype");
        }
        if (color == null || devicetype == null) {
            throw new IllegalStateException("Bot address XML entries require color and devicetype attributes.");
        }
        if (parser.isEmptyElementTag()) {
            throw new IllegalStateException("Bot address XML entries should not be empty element tags.");
        }
        // record the IP address

        while (parser.getEventType() != parser.END_TAG){
            if(parser.getEventType() == parser.TEXT){
                deviceAddresses.put(new BotInfoXmlEntry(color, null, devicetype), parser.getText());
            }
            parser.next();
        }
    }

    // reads bot_names.xml to determine which names correspond to each color
    private void readNames(XmlPullParser parser) throws IOException, XmlPullParserException {
        String color = null;
        if (parser.getAttributeCount() >= 1) {
            color = parser.getAttributeValue(null, "color");
        }
        if (color == null) {
            throw new IllegalStateException("Bot name XML entries require a color attribute.");
        }
        if (parser.isEmptyElementTag()) {
            throw new IllegalStateException("Bot address XML entries should not be empty element tags.");
        }
        // record the bot name
        while (parser.getEventType() != parser.END_TAG){
            if(parser.getEventType() == parser.TEXT){
                botNames.put(color, parser.getText());
            }
            parser.next();
        }

    }

    /**
     * Allows passing of methods that take a parser.
     */
    private static abstract class ParserTask {
        public abstract void run(XmlPullParser parser) throws XmlPullParserException, IOException;
    }

    /**
     * Allows using xml attributes as a map key
     */
    private static class BotInfoXmlEntry {
        final String color;
        final String modelType;
        final String deviceType;

        BotInfoXmlEntry(String color, String modelType, String deviceType) {
            this.color = color;
            this.modelType = modelType;
            this.deviceType = deviceType;
        }

        public int hashCode() {
            return Objects.hash(color, modelType, deviceType);
        }

        public boolean equals(Object other) {
            if (!(other instanceof BotInfoXmlEntry)) { return false; }
            BotInfoXmlEntry entry = (BotInfoXmlEntry)other;
            return Objects.equals(color, entry.color)
                    && Objects.equals(modelType, entry.modelType)
                    && Objects.equals(deviceType, entry.deviceType);
        }

        public String toString() {
            return "(" + color + ", " + modelType + ", " + deviceType + ")";
        }
    }
}
