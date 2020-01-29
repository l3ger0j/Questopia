package com.qsp.player;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.stock.GameItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pl.droidsonroids.gif.GifDrawable;

public class Utility {

    public static Spanned AttachGifCallback(Spanned html, Drawable.Callback callback) {
        boolean updated = false;
        SpannableStringBuilder ssb = new SpannableStringBuilder(html);
        for (ImageSpan img : ssb.getSpans(0, ssb.length(), ImageSpan.class)) {
            Drawable d = img.getDrawable();
            if (d instanceof GifDrawable) {
                GifDrawable gd = (GifDrawable) d;
                gd.setCallback(callback);
                gd.start();
                updated = true;
            }
        }
        return updated ? ssb : html;
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        int desiredWidth = MeasureSpec.makeMeasureSpec(listView.getWidth(), MeasureSpec.AT_MOST);
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    public static String ConvertGameTitleToCorrectFolderName(String title) {
        // Обрезаем многоточие
        String folder = title.endsWith("...") ? title.substring(0, title.length() - 3) : title;
        // Меняем двоеточие на запятую
        folder = folder.replace(':', ',');
        // Убираем кавычки
        folder = folder.replace('"', '_');
        // Убираем знак вопроса
        folder = folder.replace('?', '_');
        // Убираем звездочку
        folder = folder.replace('*', '_');
        // Убираем вертикальную черту
        folder = folder.replace('|', '_');
        // Убираем знак "меньше"
        folder = folder.replace('<', '_');
        // Убираем знак "больше"
        folder = folder.replace('>', '_');

        folder = folder.replace(' ', '_');
        folder = folder.replace("__", "_");
        return folder;
    }

//Replacing this code with QspStrToWebView
    public static Spanned QspStrToHtml(String str, ImageGetter imgGetter, String srcDir, int maxW, int maxH, boolean fitToWidth, boolean hideImg,Context uiContext) {
Utility.WriteLog("Original (StrToHtml): "+str);
        if (str != null && str.length() > 0) {
            str = htmlizeLineBreaks(str);
            str = str.replaceAll("(?i)</td>", "");
            str = str.replaceAll("(?i)</tr>", "<br>");

            str = minimizeBreaks(str);

            str = fixTableAlign(str);

            str = fixImagesSize(str,srcDir,true,maxW,maxH,fitToWidth, hideImg, uiContext);

Utility.WriteLog("toHtml:\n"+ str);
            return Html.fromHtml(str, imgGetter, null);

        }

        return Html.fromHtml("");
    }

    public static String QspStrToWebView(String str, String srcDir, int maxW, int maxH, boolean audioIsOn,boolean fitToWidth, boolean videoSwitch, boolean hideImg, Context uiContext) {
        if (str != null && str.length() > 0) {
//            Utility.WriteLog("Orig. Text: "+str);
            str = htmlizeLineBreaks(str);

            str = fixTableAlign(str);

            str = fixImagesSize(str,srcDir,false,maxW,maxH,fitToWidth,hideImg, uiContext);

            str = fixVideosLinks(str,srcDir,maxW,maxH,audioIsOn,fitToWidth,hideImg,uiContext);

            if (videoSwitch && !hideImg)
                str = useVideoBeforeImages(str,audioIsOn,videoSwitch, uiContext);

//            str = QspPlayerStart.freshPageURL.replace("REPLACETEXT", str);
//Utility.WriteLog("toWebView:\n"+ str);

            return str;

        }
        return "";
    }

    private static String htmlizeLineBreaks(String s) {
        return s.replaceAll("\n", "<br>")
                .replaceAll("\r", "");
    }

    //Remove <br> at beginning of page, combine <br><br> into single break
    private static String minimizeBreaks (String str) {
        String newStr = str;

        //Remove <br> (and leading whitespace characters) at start
        //Replace </center><br> with just </center>
        //Remove extra breaks near tables, table rows, and div tags
        Pattern pattern = Pattern.compile("^[\\s]*<br>|</center>[\\s]*<br>|<table.*><br>|</?t.><br>|</div><br>");
        Matcher matcher = pattern.matcher(str);

        int i=0;
        while (matcher.find()) {
            String group = matcher.group();

            String newGroup = group.replaceFirst("<br>","");

            Utility.WriteLog("Matcher is"+(i++)+": "+group);
            Utility.WriteLog("Changed to"+(i)+": "+newGroup);

            newStr = newStr.replaceFirst(group,newGroup);
        }

        Utility.WriteLog("Orig. Text2: "+newStr);
        //Replace <br><br> (plus leading whitespace/intervening spaces) with single <br>
        newStr = newStr.replaceAll("[\\s]*<br>[ ]*<br>","<br>");

/*
        //Remove <br> (and leading whitespace characters) at start
        newStr = newStr.replaceFirst("^[\\s]*<br>","");

        //Replace </center><br> with just </center>
        newStr = newStr.replaceAll("</center>[\\s]*<br>","</center>");

        //Replace <br><br> (plus leading whitespace/intervening spaces) with single <br>
        newStr = newStr.replaceAll("[\\s]*<br>[ ]*<br>","<br>");

        //Remove extra breaks in tables and table rows
        newStr = newStr.replace("<table><br>","<table>");
        newStr = newStr.replace("<tr><br>","<tr>");
        newStr = newStr.replace("</td><br>","</td>");
*/
        return newStr;
    }

    //Move certain <table>-bound attributes to the appropriate <tr> tag
    private static String fixTableAlign (String str) {

        String newStr = "";
        String curStr = "";
        int inTable = 0;
        Stack<String[]> attribs = new Stack<String[]>();
        Stack<Integer> startOfTable = new Stack<Integer>();

        boolean result = true;
        int openTableIdx = 0;
        int closeTableIdx = 0;
        int curIdx = 0;
        int lastIdx = 0;
        String[] curStyles;

        do {
            openTableIdx = str.indexOf("<table",curIdx);
            closeTableIdx = str.indexOf("</table",curIdx);
            curStyles = new String[0];

            //if no more open/close Table, end this mess
            if ((openTableIdx < 0) && (closeTableIdx < 0))
            {
                result = false;
                continue;
            }
            //If closeTable comes before openTable
            else if ( ((openTableIdx < 0) || (closeTableIdx < openTableIdx)) &&
                        (closeTableIdx >= 0) )
            {
                if (inTable > 0)
                {
                    //If there is a table attribute on the stack, pop it and perform the table
                    //attribute encoding
                    if (!attribs.isEmpty())
                    {
                        curStyles = attribs.pop();
//                        int curStart = startOfTable.pop();
                    }
                    inTable--;
                }

                //Mark the current str (start to finish) and make a curStr to process
                lastIdx = curIdx;
                curIdx = closeTableIdx+7;
                curStr = str.substring(lastIdx,curIdx);
            }

            //If openTable before closeTable
            else if ( (openTableIdx >= 0) &&
                    ((openTableIdx < closeTableIdx) || (closeTableIdx < 0)) )
            {
                int openTableEnd = str.substring(openTableIdx).indexOf(">");
                //Add the attributes for this table to the stack
                if (openTableEnd > 0 ) {
                    curStyles = getStyles(str.substring(openTableIdx, openTableEnd+openTableIdx));

                    //Attributes are cumulative for nested tables, so add previous attributes
                    if (!attribs.isEmpty()) {
//Utility.WriteLog("Merging ["+StringArrayToString(curStyles)+"] + ["+StringArrayToString(attribs.peek())+"]");
                        curStyles = MergeStrArrays(curStyles, attribs.peek());
                    }

                    attribs.push(curStyles);
//                    startOfTable.push(openTableEnd);
                }
                //if there's no end bracket for the table tag, end the encoding
                else return newStr;
                inTable++;

                //Mark the current str (start to finish) and make a curStr to process
                lastIdx = curIdx;
                curIdx = openTableIdx+6;
                curStr = str.substring(lastIdx,curIdx);
            }

            //Once opening or closing the table is completed, process/attribute the curStr
//Utility.WriteLog("total curStyles: "+curStyles.length+", Index: "+lastIdx+" to "+curIdx);
//Utility.WriteLog("curStyles: "+StringArrayToString(curStyles));

            if (curStyles != null) {
                if (curStyles.length != 0)
                    newStr += addTagStyles(curStr, curStyles);
                else
                    newStr += curStr;
            }
            else
                newStr += curStr;

        } while (result);

        //add the remainder of str, if any
        if (curIdx < str.length())
            newStr += str.substring(curIdx,str.length());

//Utility.WriteLog("post-TableAlign: "+newStr);
        return newStr;
    }

    private static String StringArrayToString (String[] target) {
        if (target.length == 0) return "";

        String newStr = "";
        for (int i=0; i<target.length; i++) {
            if (isNullOrEmpty(target[i])) continue;
            newStr += " "+target[i];
        }
        if (newStr.startsWith(" ")) newStr = newStr.substring(1);

        return newStr;
    }

    private static String addTagStyles (String target,String[] styles) {
        if (isNullOrEmpty(target)) return "";
        if (styles.length == 0) return target;

        for (int i=0; i<styles.length; i++) {
            //if the string is null/empty or is not "X=Y", skip
            if (isNullOrEmpty(styles[i])) continue;
            int attribIdx = styles[i].indexOf("=");
            if (attribIdx < 1) continue;

//Utility.WriteLog("Switch: "+styles[i].substring(0,attribIdx));
            switch ( styles[i].substring(0,attribIdx) ) {
                //if table has valign, insert it in the "<tr" and "<td" tags
                case "valign": {
//Utility.WriteLog("Valign active");
                    target = target.replace("<tr","<tr "+styles[i]+" ");
                }

            }

        }

        return target;
    }

    public static String[] MergeStrArrays (String[] array1, String[] array2) {
        int len1 = array1.length;
        int len2 = array2.length;
        if ((len1 == 0) && (len2 == 0)) return null;
        if ((len1 > 0) && (len2 == 0)) return array1;
        if ((len1 == 0) && (len2 > 0)) return array2;

        String[] newArray = new String[len1+len2];
        int totalItems = 0;

        //Concatenate both arrays, but don't duplicate attributes or add non-attributes
        for (int i=0; i<len1; i++) {
            //Verify the item is an attribute ("X=Y")
            int attribIdx = array1[i].indexOf("=");
            if (attribIdx < 1) continue;

            //If the attribute is in the array, skip
            boolean nextItem = false;
            for (int j=0; j<totalItems; j++)
                if (newArray[j].startsWith(array1[i].substring(0,attribIdx)))
                    nextItem = true;
            if (nextItem) continue;

            //Add the item to the array
            newArray[totalItems] = array1[i];
            totalItems++;
        }

        for (int i=0; i<len2; i++) {
            //Verify the item is an attribute ("X=Y")
            int attribIdx = array2[i].indexOf("=");
            if (attribIdx < 1) continue;

            //If the attribute is in the array, skip it
            boolean nextItem = false;
            for (int j=0; j<totalItems; j++)
                if (newArray[j].startsWith(array2[i].substring(0,attribIdx))) nextItem = true;
            if (nextItem) continue;

            //Add the item to the array
            newArray[totalItems] = array2[i];
            totalItems++;
        }

        String[] finalArray = new String[totalItems];
        for (int i=0; i<totalItems; i++)
            finalArray[i] = newArray[i];
        return finalArray;

    }

    private static String[] getStyles (String tableTag) {
        //first, change the table to remove the spaces around "="
        tableTag = tableTag.replaceAll("[ ]*=[ ]*","=");

        //Check for each style attribute in format "X=Y"
        Pattern pattern = Pattern.compile("(\\S+)=['\"]?((?:(?!/>|>|\"|'|\\s).)+)");
        Matcher matcher = pattern.matcher(tableTag);
        List<String> styleList = new ArrayList<String>();

        //Add them one-by-one as separate strings, then return an array of style tags
        int i=0;
        while (matcher.find()) {
            String group = matcher.group();
            styleList.add(group);
            i++;
        }
        if (styleList.isEmpty()) {
            String[] tempBin = {};
            return tempBin;
        }
        else
            return styleList.toArray(new String[styleList.size()]);
    }

    public static String prepareForExec(String code) {
        return new String(Base64.decode(code, Base64.DEFAULT));
    }

    public static String encodeExec(String str) {
        StringBuilder sb = new StringBuilder();
        int startIndex = 0;
        do {
            int execIndex = str.indexOf("exec:", startIndex);
            if (execIndex == -1) {
                sb.append(str.substring(startIndex));
                break;
            }
            else {
                sb.append(str, startIndex, execIndex + 5);
                int quoteIndex = str.indexOf('"', execIndex + 5);
                if (quoteIndex == -1) {
                    throw new RuntimeException("Closing quote not found for exec at " + execIndex);
                }
                String code = str.substring(execIndex + 5, quoteIndex);
                String b64code = Base64.encodeToString(code.getBytes(), Base64.NO_WRAP);
                sb.append(b64code);
                sb.append('"');
                startIndex = quoteIndex + 1;
            }
        } while (true);

        return sb.toString();
    }

    public static String fixImagesSize(String str, String srcDir, boolean isForTextView, int maxW, int maxH, boolean fitToWidth, boolean hideImg, Context uiContext) {
        boolean hasImg = str.contains("<img");
        boolean countedImg = false;
        int inTable = 0;
        int imgsInLine = 0;
        int imgCount = 0;

        int fisCycles = 0;

//Utility.WriteLog("fixImagesSize: "+str);

        if (!hasImg) return str;
        Resources res = uiContext.getResources();
        String endOfStr = str;
        String newStr= str;
        Pattern pattern = Pattern.compile("(\\S+)=['\"]?((?:(?!/>|>|\"|'|\\s).)+)");
        do {
            int firstImg = endOfStr.indexOf("<img");
            if (firstImg==-1) {
                hasImg=false;
                continue;
            }

            //First, if there is a table starting/ending, take care of it
            // ** START TABLE CHECK **
            int openTable = endOfStr.indexOf("<table");
            int closeTable = endOfStr.indexOf("</table");
//Utility.WriteLog("open/close "+fisCycles+": "+openTable+", "+closeTable);

            //if <table is found before <img or </table, inTable++
            if ((openTable >= 0)
                    && (openTable < firstImg)
                    && ( (openTable < closeTable) || (closeTable < 0) )) {
                inTable++;
//Utility.WriteLog("inTable+ = "+inTable+", "+openTable);

                String tableStr = endOfStr.substring(openTable);
                int endTableTag = tableStr.indexOf(">");
                if (endTableTag > 0)
                    tableStr = tableStr.substring(0,endTableTag+1);

                endOfStr = endOfStr.substring(openTable+tableStr.length());

                newStr += tableStr;
                continue;
            }
            //if inside a <table> AND </table is found before <img or <table, inTable--
            if ((inTable > 0)
                    && (closeTable >= 0)
                    && (closeTable < firstImg)
                    && ((closeTable < openTable) || (openTable < 0))) {
                inTable--;
                Utility.WriteLog("inTable- = "+inTable+", "+closeTable);

                String tableStr = endOfStr.substring(closeTable);
                int endTableTag = tableStr.indexOf(">");
                tableStr = tableStr.substring(0,endTableTag+1);
                endOfStr = endOfStr.substring(closeTable+tableStr.length());

                newStr += tableStr;
                continue;
            }
            // ** END TABLE CHECK **

            //Second, if this is a new line (after a <br> or very start of the string), count all
            //the <img tags for this line; skip if currently inside a table
            // ** START OF COUNT IMGS FOR LINE
            if (inTable == 0) {
                int breakIndex = endOfStr.indexOf("<br");
                //all <br> before <img covered, so count <img if <img not counted yet
                if (!countedImg) {
                    //count to the next <br, or end of string if no more <br
                    if ((breakIndex < 0))
                        breakIndex = endOfStr.length();
                    String imgStr = endOfStr.substring(0,breakIndex);

                    imgsInLine = getImgsInLine(imgStr);

                    countedImg = true;
//Utility.WriteLog("imgStr = " + imgStr);
//Utility.WriteLog("imgsInLine = " + imgsInLine);
                }
                //if <br> comes before <img AND already counted images
                if ((breakIndex >= 0) && (breakIndex < firstImg) && (countedImg)) {
                    countedImg = false;

                    String breakStr = endOfStr.substring(breakIndex);
                    int endBreak = breakStr.indexOf(">");
                    breakStr = breakStr.substring(0, endBreak + 1);
                    endOfStr = endOfStr.substring(breakIndex + breakStr.length());

                    newStr += breakStr;
                    continue;
                }
            }
            //** END OF COUNT IMGS FOR LINE

            hasImg = firstImg >=0;
            String curStr = endOfStr.substring(firstImg);
            int endImg = curStr.indexOf(">");
            if (endImg<0) return newStr;
            curStr = curStr.substring(0,endImg+1);
            endOfStr = endOfStr.substring(firstImg+curStr.length());

            newStr = newStr.substring(0,newStr.indexOf(curStr));

            //Remove img spaces so that all "X...=" become "X="; this will enable better matching
//Utility.WriteLog("Before space removal: "+curStr);
            curStr = curStr.replaceAll("\\s*=\\s*","=");
//Utility.WriteLog("After space removal: "+curStr);

            Matcher matcher = pattern.matcher(curStr);


            if (matcher.groupCount()==0) continue;

            String src = null, widthS = null, heightS = null, widthBase = null, heightBase = null, usemapImg = null, altImg = null, quoteTag = "\"";
            try {
                while (matcher.find()) {
                    String group = matcher.group();

                    if (group.toLowerCase().startsWith("src=")) {
                        if (group.length()>4) {
                            src = "src=" + group.substring(4);
                            if (src.contains("\"") || src.contains("'"))
                                src = GetFullSrc(curStr,src);

                            if (src.charAt(4)=='\'')
                                quoteTag = "'";

                            else if (src.charAt(4) != quoteTag.charAt(0))
                                src = src.replace("src=", "src=\"");
                        }
                    }
                    else if (group.toLowerCase().startsWith("width=")) {
                        widthBase = group;
                        widthS = group.substring(6);
                    }
                    else if (group.toLowerCase().startsWith("height=")) {
                        heightBase = group;
                        heightS = group.substring(7);
                    }
                    else if (group.toLowerCase().startsWith("usemap=\"")) {
                        if (group.length()>7)
                            usemapImg = "usemap="+group.substring(7);
                    }
                }

                if (isNullOrEmpty(src)) {
                    newStr += curStr + endOfStr;
                    continue;
                }


                //Initial img string with src and map
                curStr = "<img "+src+quoteTag+" i"+(imgCount++)+">";

                //if this is for a TextView, don't use a URL locator
                if (isForTextView) {
                    String iconSrc = src;
                    //change [src=..."] to [src="]
                    if (iconSrc.indexOf(quoteTag) > 3)
                        iconSrc = iconSrc.substring(0, iconSrc.indexOf("src=") + 4) + iconSrc.substring(iconSrc.indexOf(quoteTag));
                    //Then add in the root directory for the game files
                    src = iconSrc.replace("src="+quoteTag+"/", "src="+quoteTag + srcDir);
                    curStr = curStr.replace(src, iconSrc);

                    newStr += curStr+endOfStr;
                    continue;
                }

                String newSrc = src;

                    //change [src=..."] to [src="]
                    if (newSrc.indexOf(quoteTag) > 3)
                        newSrc = newSrc.substring(0, newSrc.indexOf("src=") + 4) + newSrc.substring(newSrc.indexOf(quoteTag));

                //Change all the spaces within the link to %20 (%20 represents a space)
                newSrc = newSrc.replace(" ","%20");
                //Change all the "&" within the link to %26 (%26 represents an ampersand)
                newSrc = newSrc.replace("&","%26");

                    //src is file URI without file://, then add file://
                    if ((newSrc.matches("^src=[\"'][/]?[[^/:][/]?]+[^/:]+"))) {
Utility.WriteLog("newSrc matches URI without file://");
                        if (newSrc.matches("^src=[\"'][^/].*")) {
                            newSrc = newSrc.replace("src="+quoteTag, "src="+quoteTag+"file://" + srcDir);
                            newSrc = CorrectJpegFileName(newSrc);
                        } else if (newSrc.matches("^src=[\"']/.*")) {
                            newSrc = newSrc.replace("src="+quoteTag+"/", "src="+quoteTag+"file://" + srcDir);
                            newSrc = CorrectJpegFileName(newSrc);
                        }
                        curStr = curStr.replace(src, newSrc);
                    }
                    //If src is file:// URI, don't do anything (placeholder for future use)
                    else if ((newSrc.matches("^src=[\"']file://[/]?[[^/:][/]?]+"))) {
                    }
                    //If src is generic URI, don't do anything (placeholder for future use)
                    else if ((newSrc.matches("^src=[\"'][a-zA-Z]+://[[^/][/]?]+"))) {
                    } else ;

                //Add an [alt=""] string with the img source
                if ((src.indexOf(quoteTag)>0)) {
                    curStr = curStr.replace("<img", "<img alt=\"ALT-IMG-TEXT\"");
                    String altStr = src.substring(src.indexOf(quoteTag)).replace(quoteTag,"");
                    curStr = curStr.replace("ALT-IMG-TEXT",altStr);
                }

                //Add in the "usemap" code, if there is one
                if (!isNullOrEmpty(usemapImg)) {
                    curStr = curStr.replace("<img", "<img "+usemapImg+quoteTag);
                }


//Utility.WriteLog(newSrc.substring(newSrc.indexOf("///")+2));
                BitmapFactory.Options imgDim = null;

                //if (false && hideImg) {
                //    imgDim = new BitmapFactory.Options();
                //    imgDim.inJustDecodeBounds = true;
                //    BitmapFactory.decodeResource(res,R.drawable.hiddenimg,imgDim);
                //}

                String fileNewSrc = newSrc.replace("%20"," ").replace("%26","&");

                if (fileNewSrc.contains("file://"))
                    if (new File(fileNewSrc.substring(fileNewSrc.indexOf("///") + 2)).exists())
                        imgDim = getImageDimFromFile(fileNewSrc.substring(fileNewSrc.indexOf("///") + 2));
                else
                    if (new File(fileNewSrc.replace("src="+quoteTag,"")).exists())
                        imgDim = getImageDimFromURI(fileNewSrc.replace("src="+quoteTag,""),uiContext);

                //Conceal the image if user is hiding graphics, but after checking size
                if (hideImg) {
                    String hideSrc = "src="+quoteTag+"file:///android_res/drawable/hiddenimg.jpg";
                    curStr = curStr.replace(newSrc, hideSrc);
                }

                if (imgDim == null) {
//Utility.WriteLog("imgDim is null");
                    newStr += curStr + endOfStr;
                    fisCycles++;
                    continue;
                }

                int h = imgDim.outHeight;
                int w = imgDim.outWidth;

//Utility.WriteLog("imgDim.outWidth = " + imgDim.outWidth + ", imgDim.outHeight = " + imgDim.outHeight);


//                if (isNullOrEmpty(widthS) && isNullOrEmpty(heightS)) {
//                    newStr += curStr.replace(">","style=\"width: 100%; max-width: "+maxW+"px; height: auto; max-height: "+maxH+"; \">") + endOfStr;
//                Utility.WriteLog("h: "+h+", maxH: "+maxH);

                if ((maxH > 0)) {
                        if ((fitToWidth && (w < maxW)) || (w > maxW)) {
                            h = Math.round(h * maxW / w);
                            w = maxW;
                        }
                        if (h > maxH) {
                            w = Math.round(w * maxH / h);
                            h = maxH;
                        }
                    }
                    //skip height adjustment if maxH <= 0 and just check for stretching a small
                    //image max width (fitToWidth) or reducing a big image to max width
                    else if ((fitToWidth && (w < maxW)) || (w > maxW))
                        w = maxW;

                //if there is a usemap tag AND image size has been changed, find the map and update
                //the coordinates using the conversion factor.
                if (imgDim != null)
                if(!isNullOrEmpty(usemapImg) && ((h != imgDim.outHeight)||(w != imgDim.outWidth))) {
                    float tempW = new Float(w);
                    float tempDimW = new Float(imgDim.outWidth);
                    float convFact = tempW/tempDimW;
//                    Utility.WriteLog("tempW="+w+", tempDimW="+imgDim.outWidth+", convFact="+convFact);
                    newStr = fixMapCoords(newStr,usemapImg,convFact);
                    curStr = fixMapCoords(curStr,usemapImg,convFact);
                }

//Utility.WriteLog("h: "+h+", maxH: "+maxH);
                    //if in a table, use 100% for the width so to not override the table
                    if (inTable > 0) {
                        //Scale the hideImg down if the height is less than maxH
                        if ((hideImg) && (h < maxH))
                            newStr += curStr.replace(">", " height=\""+h+"\" max-width=100%>") + endOfStr;
                        else if (h == maxH)
                            newStr += curStr.replace(">"," height=\""+maxH+"\" max-width=100%>") + endOfStr;
                        else
                            newStr += curStr.replace(">", " width=\"100%\">") + endOfStr;
                    }
                    //if not in a table AND more than one image in the line AND Image Fit to Width,
                    //multiply all the images by 1/imgsInLine. Otherwise, let WebView handle it
                    else {
                        if ((imgsInLine > 1) && (fitToWidth)) {
                            w = Math.round(w/imgsInLine);
                            h = Math.round(h/imgsInLine);
                        }
                        //Scale the hideImg to height or width, based on whether original image was scaled that way
                        if (hideImg) {
                            if (maxH < 0)
                                newStr += curStr.replace(">", " width=\"" + w + "\">") + endOfStr;
                            else
                                newStr += curStr.replace(">", " height=\"" + h + "\">") + endOfStr;
                        }
                        else if (maxH > 0) //add width and height if maxH > 0
                            newStr += curStr.replace(">", " width=\"" + w + "\" height=\"" + h + "\">") + endOfStr;
                        else //add width only if maxH <= 0
                            newStr += curStr.replace(">", " width=\"" + w + "\" >") + endOfStr;
                    }
                    fisCycles++;
//Utility.WriteLog("endOfStr "+fisCycles+": "+endOfStr);
                    continue;


            } catch (Exception e) {
                Log.e("fixImagesSize","unable to parse "+curStr,e);
            }

        } while (hasImg);
//Utility.WriteLog("newStr: "+newStr);
        return newStr;
    }

    private static String GetFullSrc (String fullStr, String src) {
        if (isNullOrEmpty(fullStr) || isNullOrEmpty(src) || !fullStr.contains(src)) return null;

        //Crop everything before src
        String newSrc = fullStr.substring(fullStr.indexOf(src));

//Utility.WriteLog("src: "+src+"\nnewSrc: "+newSrc);

        //Find the first " or '; return src if there is no " or '
        boolean singleQuote = false;
        int firstQuote = newSrc.indexOf("\"");
        if (firstQuote < 0) {
            firstQuote = newSrc.indexOf("'");
            if (firstQuote < 0) return src;
            singleQuote = true;
        }
//Utility.WriteLog("firstQuote: "+firstQuote+", singleQuote: "+singleQuote);
        //Stop if the quote is at the end of the string
        if (newSrc.length() < firstQuote+2) return src;

        //Find the next " or '; return src if there's no second " or '
        int nextQuote;
        if (singleQuote) {
            nextQuote = newSrc.indexOf("'",firstQuote+1);
            if ((nextQuote < 0)) return src;
        }
        else {
            nextQuote = newSrc.indexOf("\"",firstQuote+1);
            if ((nextQuote < 0)) return src;
        }

Utility.WriteLog("firstQuote: "+firstQuote+", singleQuote: "+singleQuote+", nextQuote: "+nextQuote);

        //Make sure the second quote is before the next =; return src if = is before the next quote
        int equalsIdx = newSrc.indexOf("=",firstQuote+1);
        if ((equalsIdx < nextQuote) && (equalsIdx != -1)) return src;

        newSrc = newSrc.substring(0,nextQuote);

Utility.WriteLog("final newSrc: "+newSrc);
        return newSrc;
    }

    //Find the <map> tag for <img usemap="X"> and fix the coordinates
    private static String fixMapCoords (String str, String usemapStr, float conversionFactor) {
Utility.WriteLog("fixMapCoords: "+str);
        //If either string is null, or usemapStr doesn't have a "#NAME", return
        if (isNullOrEmpty(str) || isNullOrEmpty(usemapStr)) {
            return str;
        }
        int trueMapIdx = usemapStr.indexOf("#");
        if ((trueMapIdx < 0) || (trueMapIdx == usemapStr.length())) return str;

        //Get the map name NAME from usemapStr from format #NAME
        String trueMapName = null;
        int trueMapEnd = usemapStr.substring(trueMapIdx).indexOf("\"");
        if (trueMapIdx+1 < trueMapEnd) trueMapName = usemapStr.substring(trueMapIdx+1,trueMapEnd).toLowerCase();
        else trueMapName = usemapStr.substring(trueMapIdx+1).toLowerCase();

        boolean hasArea = str.contains("<area");
        if (!hasArea) return str;
        String newMapStr = str;
        int insideMap = 0;
        String endMapStr = str;
        String curMapName = null;
        Pattern pattern = Pattern.compile("(\\S+)=['\"]?((?:(?!/>|>|\"|'|\\s).)+)");

        //Examine all <area tags within str that are exactly one <map tag deep
        do {
            //If there area no more <area tags, finish the check
            int firstArea = endMapStr.indexOf("<area");
            if (firstArea==-1) {
                hasArea=false;
                continue;
            }

            //First, find a <map region; cannot perform area fix outside the map region
            // ** START MAP CHECK **
            int openMap = endMapStr.indexOf("<map");
            int closeMap = endMapStr.indexOf("</map");

            //if <map is found before <area or </map, insideMap++; record the map "name" if only
            //one map tag deep (ignore embedded maps)
            if ((openMap >= 0)
                    && (openMap < firstArea)
                    && ( (openMap < closeMap) || (closeMap < 0) )) {
                insideMap++;
                Utility.WriteLog("insideMap+ = "+insideMap+", "+openMap);

                String mapTagStr = endMapStr.substring(openMap);
                int endTableTag = mapTagStr.indexOf(">");
                if (endTableTag > 0) {
                    mapTagStr = mapTagStr.substring(0, endTableTag + 1);
                    if (insideMap == 1) curMapName = getTagName(mapTagStr).toLowerCase();
                }

                endMapStr = endMapStr.substring(openMap+mapTagStr.length());

                newMapStr += mapTagStr;
                continue;
            }

            //if inside a <map> AND </map is found before <area or <map, insideMap--; if no longer
            //in a map, clear the map "name"
            if ((insideMap > 0)
                    && (closeMap >= 0)
                    && (closeMap < firstArea)
                    && ((closeMap < openMap) || (openMap < 0))) {
                insideMap--;
Utility.WriteLog("insideMap- = "+insideMap+", "+closeMap);

                String mapTagStr = endMapStr.substring(closeMap);
                int endTableTag = mapTagStr.indexOf(">");
                mapTagStr = mapTagStr.substring(0,endTableTag+1);
                endMapStr = endMapStr.substring(closeMap+mapTagStr.length());

                if (insideMap == 0) curMapName = null;

                newMapStr += mapTagStr;
                continue;
            }
            // ** END MAP CHECK **

Utility.WriteLog("End Map Check, Start Area Check");

            // ** BEGIN AREA TAG PROCESSING **
            hasArea = firstArea >=0;
            String curMapStr = endMapStr.substring(firstArea);
            int endArea = curMapStr.indexOf(">");

            //if <area tag doesn't end, end the process immediately
            if (endArea<0) return newMapStr;

            //Setup newMapStr, newCurStr, and endMapStr for the <area...> tag
            curMapStr = curMapStr.substring(0,endArea+1);
            endMapStr = endMapStr.substring(firstArea+curMapStr.length());
            newMapStr = newMapStr.substring(0,newMapStr.indexOf(curMapStr));

            //If this area is not within a first level map (insideMap == 1), or there is no
            //usable map name, skip this area
            if ((insideMap != 1) || isNullOrEmpty(curMapName)) {
                newMapStr += curMapStr + endMapStr;
                continue;
            }

            //Remove area spaces so that all "X...=" become "X="; this will enable better matching
Utility.WriteLog("Before <area> space removal: "+curMapStr);
            curMapStr = curMapStr.replaceAll("\\s*=\\s*","=");
Utility.WriteLog("After <area> space removal: "+curMapStr);

            Matcher matcher = pattern.matcher(curMapStr);
            if (matcher.groupCount()==0) continue;

            //Grab the coordinates attribute
            String coordStr = null;
            ArrayList<Integer> coordInt = new ArrayList<Integer>();
            try {
                while (matcher.find()) {
                    String group = matcher.group();
                    if (group.toLowerCase().startsWith("coords=\"")) {
                        if (group.length() > 8)
                            coordStr = "coords=\""+group.substring(8);
                    }
                }

Utility.WriteLog("coordStr: "+coordStr+"||trueMapName: "+trueMapName+"||curMapName: "+curMapName);
                //if no coordinates present or the map name doesn't match, skip this area tag
                if (isNullOrEmpty(coordStr) || !curMapName.equals(trueMapName)) {
                    newMapStr += curMapStr + endMapStr;
                    continue;
                }

                //Look for numbers within the coordinates group; catch possible decimal places
                Pattern p = Pattern.compile("\\d+[.\\d+]?");
                Matcher m = p.matcher(coordStr);
                while (m.find()) {
                    String tempIntAsStr = m.group();
                    //Dump decimal places if present (round down)
                    if (tempIntAsStr.contains("."))
                        tempIntAsStr = tempIntAsStr.substring(0,tempIntAsStr.indexOf("."));
Utility.WriteLog("Found tempInt: "+tempIntAsStr);
                    int tempInt = Integer.parseInt(tempIntAsStr);

                    //Apply the conversion factor for the area coordinates
                    float tempFloat = (tempInt*conversionFactor);
                    tempInt = Math.round(tempFloat);
Utility.WriteLog("tempIntAsStr: "+tempIntAsStr+" * convFact: "+conversionFactor+" = tempFloat: "+tempFloat+" = tempInt: "+tempInt);

                    coordInt.add(tempInt);
                }

Utility.WriteLog("Total coords found: "+coordInt.size());

                //Skip this area if there were no coordinates to convert
                if (coordInt.isEmpty()) {
                    newMapStr += curMapStr + endMapStr;
                    continue;
                }

                //Fabricate the new coordinates string from the new coordinate values
                String newCoords = "coords=\"" + coordInt.get(0);
                for (int i=1; i<coordInt.size(); i++)
                    newCoords += ","+coordInt.get(i);

Utility.WriteLog("curMapStr: "+curMapStr);
Utility.WriteLog("coordStr: "+coordStr+", newCoords: "+newCoords);

                //replace the curMapStr coordinates with the new version
                curMapStr = curMapStr.replace(coordStr,newCoords);

            } catch (Exception e) {
                Log.e("fixMapCoords","unable to parse "+curMapStr,e);
            }

            newMapStr += curMapStr + endMapStr;
        } while (hasArea);


        return newMapStr;
    }

    //Takes an HTML tag and looks for the first name="X", and returns X
    private static String getTagName (String tagStr) {
Utility.WriteLog("getTagName from: "+tagStr);
        if (isNullOrEmpty(tagStr)) return null;
        if (!tagStr.toLowerCase().contains("name")) return null;

        Pattern pattern = Pattern.compile("(\\S+)=['\"]?((?:(?!/>|>|\"|'|\\s).)+)");
        Matcher matchTag = pattern.matcher(tagStr);
        if (matchTag.groupCount()==0) return null;

        String tagName = null;
        try {
            while (matchTag.find()) {
                String group = matchTag.group();
                if (group.toLowerCase().startsWith("name=\"")) {
                    tagName = group.substring(6);
                }
            }
        } catch (Exception e) {
            Log.e("getTagName","unable to parse "+tagStr,e);
        }
Utility.WriteLog("tagName: "+tagName);
        return tagName;
    }

    private static String CorrectJpegFileName (String src) {
        String newSrc = src;
        //trueSrc will be the file URL;
        //skip this <img if it's not a file:// OR if there's no ".abc" at the end
        String trueSrc = src.replace("src=\"file://","");
//Utility.WriteLog("trueSrc: "+trueSrc);
        int fileNameIdx = trueSrc.lastIndexOf("/")+1;
        int suffixIdx = trueSrc.lastIndexOf(".");

//Utility.WriteLog("fileNameIdx "+fileNameIdx+", suffixIdx "+suffixIdx);
        if ((suffixIdx < 1) || (suffixIdx < fileNameIdx) || (suffixIdx < trueSrc.length()-4) ) {
            return newSrc;
        }

        //Get list of files in <img directory with similar prefix
        File imgFile = new File(trueSrc);
        File imgDir = new File(trueSrc.substring(0,trueSrc.lastIndexOf("/")));
        if (!imgDir.exists() || imgFile.exists()) {
            return newSrc;
        }

        String tempFileName = imgFile.getName();
        int fileSuffixIdx = tempFileName.lastIndexOf(".");
        if (fileSuffixIdx < 0) {
            return newSrc;
        }
        final String filePrefix = tempFileName.substring(0,fileSuffixIdx);
//Utility.WriteLog("imgDir isFile = " + imgFile.isFile());
//Utility.WriteLog("filePrefix = " + filePrefix);
//Utility.WriteLog("imgDir isDirectory = " + imgDir.isDirectory());
        File[] altFileList = imgDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("^"+filePrefix+"\\.[\\w]+");
            }
        });
//Utility.WriteLog("Total files found = "+altFileList.length);


        //Check each file;
        boolean changeExt = false;
        boolean imgChanged = false;
        int extValue = -1;
        for (int i=0; (i<altFileList.length) || imgChanged;i++) {
            //Test if current file is a video file
            String altFileName = altFileList[i].getName();
            String altFileExt = altFileName.substring(altFileName.indexOf(".")+1);
            MimeTypeMap myMTM = MimeTypeMap.getSingleton();
            String tempFileType = myMTM.getMimeTypeFromExtension(altFileExt);
//Utility.WriteLog(altFileList[i].getName()+" extension is "+altFileExt+" and is "+tempFileType);
            //if no type, skip
            if (isNullOrEmpty(tempFileType)) continue;

            //If fileType okay, check images by preference: [gif] > [jpg/jpeg] > [png] > [other]
            if (tempFileType.startsWith("image")) {
                switch (altFileExt.toLowerCase()) {
                    case "gif":
                        if (extValue < 30) {
                            changeExt = true;
                            extValue = 30;
                        }
                        break;
                    case "jpg": case "jpeg":
                        if (extValue < 20) {
                            changeExt = true;
                            extValue = 20;
                        }
                        break;
                    case "png":
                        if (extValue < 10) {
                            changeExt = true;
                            extValue = 10;
                        }
                        break;
                    default:
                        if (extValue < 0) {
                            changeExt = true;
                            extValue = 0;
                        }
                        break;
                }
                if (changeExt)
                    newSrc = newSrc.replace(src, src.substring(0, src.lastIndexOf(".") + 1) + altFileExt);
                changeExt = false;
            }

        }
        return newSrc;
    }

    private static BitmapFactory.Options getImageDimFromURI (String imgURI, Context uiContext) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;

        try {
            BitmapFactory.decodeStream(
                    uiContext.getContentResolver().openInputStream(Uri.parse(imgURI)),
                    null,
                    opt);
        }
        catch (FileNotFoundException e) {
            return null;
        }

        return opt;
    }

    private static BitmapFactory.Options getImageDimFromFile (String imgSrc) {
        File imgFile = new File(imgSrc);
        //if the imgFile doesn't exist, return null
        if(!imgFile.exists()) {
            Utility.WriteLog("Image File doesn't exist");
            return null;
        }
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgSrc, opt);

        return opt;
    }

    private static int getImgsInLine (String imgStr) {
        int firstImgIndex = 0;
        int endImgIndex = 0;
        int totalImages = 0;
        boolean spacePresent = false;

        while (firstImgIndex != -1) {
            //mark the start of the <img> tag
            firstImgIndex = imgStr.indexOf("<img", firstImgIndex);

//            Utility.WriteLog("totalImages: "+totalImages+", firstImgIndex: "+firstImgIndex+", endImageIndex: "+endImgIndex+", difference = "+(firstImgIndex-endImgIndex));
            //if there are multiple images and there are spaces or characters between the images,
            //treat the images as if on separate lines
            if ((firstImgIndex >= 0) && (firstImgIndex-endImgIndex > 1)) spacePresent = true;
            if ((totalImages > 1) && (spacePresent))
                return 1;

            //if there are no breaks between images, mark the end of the <img> tag
            endImgIndex = imgStr.indexOf(">", firstImgIndex);

            //add up each image as it comes
            if (firstImgIndex != -1) {
                totalImages++;
                firstImgIndex += "<img".length();
            }

        }

        return totalImages;
    }

    private static String fixVideosLinks (String str, String srcDir, int maxW, int maxH,boolean audioIsOn, boolean fitToWidth, boolean hideImg, Context uiContext) {
        int vidCount = 0;
        int inVideo = 0;
        boolean hasVid = str.contains("<video");
        boolean hasVidExit = str.contains("</video");
        boolean hasSrc = str.contains("<source");

        String endOfStr = str;
        String newStr= str;
        if (!hasVid) return str;

        Pattern pattern = Pattern.compile("(\\S+)=['\"]?((?:(?!/>|>|\"|'|\\s).)+)");
        do {
            int firstVid = endOfStr.indexOf("<video");
            int exitVid = endOfStr.indexOf("</video");
            int firstSrc = endOfStr.indexOf("<source");
            hasVidExit = exitVid >= 0;
            hasSrc = firstSrc >= 0;
Utility.WriteLog("firstVid: "+firstVid+", exitVid: "+exitVid+", firstSrc: "+firstSrc);

            //If in a <video tag, check for the next <video, </video, or <source and process
            //the earliest one first
            //Check </video
            if ((hasVidExit)
                    && ((exitVid < firstSrc) || (firstSrc < 0))
                    && ((exitVid < firstVid) || (firstVid < 0))) {

                String curStr = endOfStr.substring(exitVid);
                int endExitVid = curStr.indexOf(">");
                if (endExitVid<0) return newStr;
                curStr = curStr.substring(0,endExitVid+1);
                endOfStr = endOfStr.substring(exitVid+curStr.length());
                newStr = newStr.substring(0,newStr.indexOf(curStr));

                if (inVideo > 0) inVideo--;
                newStr +=curStr + endOfStr;
                continue;
            }

            //If next is a <source tag, process only if inside a <video tag
            if ((hasSrc)
                    && (inVideo > 0)
                    && ((firstSrc < exitVid) || (exitVid < 0))
                    && ((firstSrc < firstVid) || (firstVid < 0))) {

                //** BEGIN <SOURCE PROCESSING **
                String curStr = endOfStr.substring(firstSrc);
                int endSrc = curStr.indexOf(">");
                if (endSrc<0) return newStr;
                curStr = curStr.substring(0,endSrc+1);
                endOfStr = endOfStr.substring(firstSrc+curStr.length());
                newStr = newStr.substring(0,newStr.indexOf(curStr));

                //Remove img spaces so that all "X...=" become "X="; this will enable better matching
//Utility.WriteLog("Before space removal: "+curStr);
                curStr = curStr.replaceAll("\\s*=\\s*","=");
//Utility.WriteLog("After space removal: "+curStr);

                //use matcher to parse, only fix src values
                Matcher matcher = pattern.matcher(curStr);

                if (matcher.groupCount()==0) continue;
                String src = null, quoteTag = "\"";
                try {
                    while (matcher.find()) {
                        String group = matcher.group();
                        if (group.toLowerCase().startsWith("src=")) {
                            if (group.length() > 4) {
                                src = "src=" + group.substring(4);
                                if (src.charAt(4) == '\'')
                                    quoteTag = "'";
                                if (src.charAt(4) != quoteTag.charAt(0))
                                    src.replace("src=","src=\"");
                            }
                            else src = "src=";
                            if (src.contains(quoteTag))
                                src = GetFullSrc(curStr, src);
                        }
                    }

                    //If no src OR src has a ":" in it (i.e., proper URI), skip this <source tag
                    if ((isNullOrEmpty(src)) || (src.contains(":"))) {
                        newStr += curStr + endOfStr;
                        continue;
                    }

                    String newSrc = src;
                    //First, remove all spaces between src= and the first quote;
                    //i.e., change [src=    "URL] to [src="URL]
                    //However, use neutral image if hiding images
                    if (!hideImg) {
                        if (newSrc.indexOf(quoteTag) > 3)
                            newSrc = newSrc.substring(0, newSrc.indexOf("src=") + 4) + newSrc.substring(newSrc.indexOf(quoteTag));

                        //then change [src="] to [src="file://] URL
                        if ((newSrc.indexOf("src="+quoteTag) == 0) && newSrc.length() > 5) {
                            if (newSrc.substring(5, 6).matches("^[a-zA-Z0-9]")) {
                                newSrc = newSrc.replace("src="+quoteTag, "src="+quoteTag+"file://" + srcDir);
                                curStr = curStr.replace(src, newSrc);
                            } else if (newSrc.indexOf("src="+quoteTag+"/") == 0) {
                                newSrc = newSrc.replace("src="+quoteTag+"/", "src="+quoteTag+"file://" + srcDir);
                                curStr = curStr.replace(src, newSrc);
                            }
                        }
                    }
                    //Use a neutral webm if user wants to hide graphics
                    else {
                        newSrc = curStr.replace(src,"src="+quoteTag+"file:///android_res/raw/hiddenimg.webm");
                        curStr = newSrc;
                    }

                } catch (Exception e) {
                    Log.e("fixVidLinks_Source","unable parse "+curStr,e);
                }

                newStr +=curStr + endOfStr;
                //** END <SOURCE PROCESSING **

                continue;
            }

            //If no more <video, exit as there are no more </video and <source
            if (firstVid==-1) {
                hasVid=false;
                continue;
            }

            //Start working on <video tag
            vidCount++; inVideo++;
            hasVid = firstVid >=0;
            String curStr = endOfStr.substring(firstVid);
            int endVid = curStr.indexOf(">");
            if (endVid < 0) return newStr;
            curStr = curStr.substring(0,endVid+1);
            endOfStr = endOfStr.substring(firstVid+curStr.length());

            newStr = newStr.substring(0,newStr.indexOf(curStr));

            //Remove img spaces so that all "X...=" become "X="; this will enable better matching
//Utility.WriteLog("Before space removal: "+curStr);
            curStr = curStr.replaceAll("\\s*=\\s*","=");
//Utility.WriteLog("After space removal: "+curStr);

            Matcher matcher = pattern.matcher(curStr);

            if (matcher.groupCount()==0) continue;

            //Make sure the video starts and loops automatically
            if (!curStr.contains(" autoplay ") && !curStr.contains(" autoplay>"))
                curStr = curStr.replace(">"," autoplay>");
            if (!curStr.contains(" loop ") && !curStr.contains(" loop>"))
                curStr = curStr.replace(">"," loop>");
            if (!audioIsOn && !curStr.contains(" muted ") && !curStr.contains(" muted>"))
                curStr = curStr.replace(">"," muted>");

            curStr = curStr.replace(">"," v"+(vidCount++)+">");

            String src = null, widthS = null, heightS = null, widthBase = null, heightBase = null, quoteTag = "\"";
            try {
                while (matcher.find()) {
                    String group = matcher.group();
                    if (group.toLowerCase().startsWith("src=")) {
                        if (group.length()>4) {
                            src = "src=" + group.substring(4);
                            if (src.charAt(4) == '\'')
                                quoteTag = "'";
                            if (src.charAt(4) != quoteTag.charAt(0))
                                src.replace("src=","src=\"");
                        }
                        else src = "src=";
                        if (src.contains(quoteTag))
                            src = GetFullSrc(curStr,src);
                    }
                    else if (group.startsWith("width=")) {
                        widthBase = group;
                        widthS = group.substring(6);
                    }
                    else if (group.startsWith("height=")) {
                        heightBase = group;
                        heightS = group.substring(7);
                    }
                }

                if (isNullOrEmpty(src)) {
                    //Make sure to close the video tag if it's not closed on the page
                    if (!endOfStr.contains("</video")) {
                        curStr = curStr.concat("</video>");
                        if (inVideo > 0) inVideo--;
                    }

                    newStr += curStr + endOfStr;
                    continue;
                }


                String newSrc = src;
                //First, remove all spaces between src= and the first quote;
                //i.e., change [src=    "URL] to [src="URL]
                //However, use neutral image if hiding images
                if (!hideImg) {
                    if (newSrc.indexOf(quoteTag) > 3)
                        newSrc = newSrc.substring(0, newSrc.indexOf("src=") + 4) + newSrc.substring(newSrc.indexOf(quoteTag));

                    //then change [src="] to [src="file://] URL
                    if ((newSrc.indexOf("src="+quoteTag) == 0) && newSrc.length() > 5) {
                        if (newSrc.substring(5, 6).matches("^[a-zA-Z0-9]")) {
                            newSrc = newSrc.replace("src="+quoteTag, "src="+quoteTag+"file://" + srcDir);
                            curStr = curStr.replace(src, newSrc);
                        } else if (newSrc.indexOf("src="+quoteTag+"/") == 0) {
                            newSrc = newSrc.replace("src="+quoteTag+"/", "src="+quoteTag+"file://" + srcDir);
                            curStr = curStr.replace(src, newSrc);
                        }
                    }
                }
                else {
                    newSrc = curStr.replace(src,"src="+quoteTag+"file:///android_res/raw/hiddenimg.webm");
                    curStr = newSrc;
                }

                // ** Remove leading quote (") character if present **
                if (!isNullOrEmpty(widthS)) {
                    if (widthS.startsWith("\"")) { widthS = widthS.substring(1); }
                }
                if (!isNullOrEmpty(heightS)) {
                    if (heightS.startsWith("\"")) { heightS = heightS.substring(1); }
                }

                int w = isNullOrEmpty(widthS) ? 0 : Integer.parseInt(widthS);
                int h = isNullOrEmpty(heightS) ? 0 : Integer.parseInt(heightS);

                //if neither width or height are present, use max values as height/width
                if (isNullOrEmpty(widthS) || isNullOrEmpty(heightS)) {
//                    if ((w <= 0) || (w > maxW)) w = maxW;
                    if (fitToWidth)
                        curStr = curStr.replace(">", " width=\"" + maxW + "\" height=\""+maxH+"\">");
                    else
                        curStr = curStr.replace(">", " width=\"" + maxW + "\" height=\""+maxH+"\">");
                    //Make sure to close the video tag if it's not closed on the page
                    if (!endOfStr.contains("</video")) {
                        curStr = curStr.concat("</video>");
                        if (inVideo > 0) inVideo--;
                    }

                    newStr += curStr + endOfStr;
                    continue;
                }

                //if width/height are set, reduce them to maxW/maxH
                if ((w > maxW) && (maxW > 0)) {
                    if (!isNullOrEmpty(heightS)) h = Math.round(h*maxW/w);
                    w = maxW;
                }
                if ((h > maxH) && (maxH > 0)) {
                    if (!isNullOrEmpty(widthS)) w = Math.round(w*maxH/h);
                    h = maxH;
                }

                if (w > 0)
                    curStr = curStr.replace(widthBase, "width=\"" + w);
                else curStr = curStr.replace(widthBase, "width=\"");
                if (h > 0)
                    curStr = curStr.replace(heightBase, "height=\"" + h);
                else curStr = curStr.replace(heightBase, "height=\"");


            } catch (Exception e) {
                Log.e("fixVidLinks","unable parse "+curStr,e);
            }

            //Make sure to close the video tag if it's not closed on the page
            if (!endOfStr.contains("</video")) {
                curStr = curStr.concat("</video>");
                if (inVideo > 0) inVideo--;
            }

            newStr += curStr + endOfStr;
            Utility.WriteLog("fixedVidLinks: "+newStr);
        } while (hasVid || hasSrc || hasVidExit);
        return newStr;

    }

    private static String useVideoBeforeImages (String str, boolean audioIsOn, boolean videoSwitch, Context uiContext) {

        boolean hasImg = str.contains("<img");

        Utility.WriteLog("useVideoBeforeImages: "+str);

        if (!hasImg) return str;
        String endOfvidStr = str;
        String vidStr = str;
        Pattern pattern = Pattern.compile("(\\S+)=['\"]?((?:(?!/>|>|\"|'|\\s).)+)");
        do {
            int firstImg = endOfvidStr.indexOf("<img");
            if (firstImg==-1) {
                hasImg=false;
                continue;
            }

            hasImg = firstImg >=0;
            String curStr = endOfvidStr.substring(firstImg);
            int endImg = curStr.indexOf(">");
            if (endImg<0) return vidStr;
            curStr = curStr.substring(0,endImg+1);
            endOfvidStr = endOfvidStr.substring(firstImg+curStr.length());

            vidStr = vidStr.substring(0,vidStr.indexOf(curStr));

            Utility.WriteLog("\nvidStr: "+vidStr+"\n");
            Utility.WriteLog("Img: "+firstImg+" to "+endImg+", vidStr: "+vidStr.length()+", curStr: "+curStr.length()+", endOfvidStr: "+endOfvidStr.length());

            Matcher matcher = pattern.matcher(curStr);

            if (matcher.groupCount()==0) {
                vidStr+=curStr + endOfvidStr;
                continue;
            }

            String src = null, altImg = null, quoteTag = "\"";
            try {
                while (matcher.find()) {
                    String group = matcher.group();
                    if (group.toLowerCase().startsWith("src=")) {
                        if (group.length()>4) {
                            src = "src=" + group.substring(4);
                            if (src.charAt(4) == '\'')
                                quoteTag = "'";
                            if (src.charAt(4) != quoteTag.charAt(0))
                                src.replace("src=","src=\"");
                        }
                        else src = null;
                        if (src.contains(quoteTag))
                            src = GetFullSrc(curStr,src);
                    }
                }

                if (isNullOrEmpty(src)) {
                    vidStr += curStr + endOfvidStr;
                    continue;
                }

                //trueSrc will be the file URL;
                //skip this <img if it's not a file:// OR if there's no ".abc" at the end
                String trueSrc = src.replace("src="+quoteTag+"file://","");
//Utility.WriteLog("trueSrc: "+trueSrc);
                int fileNameIdx = trueSrc.lastIndexOf("/")+1;
                int suffixIdx = trueSrc.lastIndexOf(".");

//Utility.WriteLog("fileNameIdx "+fileNameIdx+", suffixIdx "+suffixIdx);
                if ((suffixIdx < 1) || (suffixIdx < fileNameIdx) || (suffixIdx < trueSrc.length()-4) ) {
                    vidStr += curStr + endOfvidStr;
                    continue;
                }

                //Get list of files in <img directory with similar prefix
                File imgFile = new File(trueSrc);
                File imgDir = new File(trueSrc.substring(0,trueSrc.lastIndexOf("/")));
                if (!imgDir.exists()) {
                    vidStr += curStr + endOfvidStr;
                    continue;
                }

                String tempFileName = imgFile.getName();
                int fileSuffixIdx = tempFileName.indexOf(".");
                if (fileSuffixIdx < 0) {
                    vidStr += curStr+endOfvidStr;
                    continue;
                }
                final String filePrefix = tempFileName.substring(0,fileSuffixIdx);
//Utility.WriteLog("imgDir isFile = " + imgFile.isFile());
//Utility.WriteLog("filePrefix = " + filePrefix);
//Utility.WriteLog("imgDir isDirectory = " + imgDir.isDirectory());
                File[] altFileList = imgDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.matches("^"+filePrefix+"\\.[\\w]+");
                    }
                });
//Utility.WriteLog("Total files found = "+altFileList.length);


                //Check each file
                boolean imgChanged = false;
                for (int i=0; (i<altFileList.length) || imgChanged;i++) {
                //Test if current file is a video file
                    String altFileName = altFileList[i].getName();
                    String altFileExt = altFileName.substring(altFileName.indexOf(".")+1);
                    MimeTypeMap myMTM = MimeTypeMap.getSingleton();
                    String tempFileType = myMTM.getMimeTypeFromExtension(altFileExt);
//Utility.WriteLog(altFileList[i].getName()+" extension is "+altFileExt+" and is "+tempFileType);
                //if no type, skip
                    if (isNullOrEmpty(tempFileType)) continue;

                    //if video okay, replace <img with <video
                    if (tempFileType.startsWith("video")) {
//Utility.WriteLog("Will replace <img with <video using "+altFileName);
                        curStr = curStr.replace("<img", "<video");
                        curStr = curStr.replace(src, src.substring(0, src.lastIndexOf(".") + 1) + altFileExt);
                        if (!audioIsOn && !curStr.contains(" muted ") && !curStr.contains(" muted>"))
                            curStr = curStr.replace(">", " muted>");
                        curStr = curStr.replace(">", " autoplay loop>Video unavailable</video>");
                        imgChanged = true;
                    }

                }

            } catch (Exception e) {
                Log.e("useVideoBeforeImages","unable to parse "+curStr,e);
            }

            vidStr += curStr + endOfvidStr;

        } while (hasImg);
Utility.WriteLog("finished vidStr = "+vidStr);
        return vidStr;
    }

    private static boolean isNullOrEmpty(String string) {
        return (string == null || string.equals(""));
    }

    public static String QspStrToStr(String str) {
//Utility.WriteLog("toStr:\n"+str);
        String result = "";
        if (str != null && str.length() > 0) {
            result = htmlizeLineBreaks(str);
        }
        return result;
    }

    public static String QspPathTranslate(String str) {
        if (str == null)
            return null;
        //In QSP, the folders are separated by the \ sign, as in DOS and Windows, for Android we translate this into /.
        //T.k. The first argument is regexp, then we escape twice.
        String result = str.replaceAll("\\\\", "/");
        result = result.replace("src=\"file:///","/");
        return result;
    }

    private static void CheckNoMedia(String path) {
        //Создаем в папке QSP пустой файл .nomedia
        File f = new File(path);
        if (f.exists())
            return;
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String GetDefaultPath(Context context) {

        //Возвращаем путь к папке с играми.
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return null;

        // ** original code for checking games directory **
        // File sdDir = Environment.getExternalStorageDirectory();
        // ** begin replacement code for checking storage directory **

        File sdDir;
        String strSDCardPath = System.getenv("SECONDARY_STORAGE");
        Utility.WriteLog("1. " + strSDCardPath);
        if ((null == strSDCardPath) || (strSDCardPath.length() == 0)) {
            strSDCardPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
            Utility.WriteLog("2. " + strSDCardPath);
        }
        if ((null == strSDCardPath) || (strSDCardPath.length() == 0)) {
            strSDCardPath = Environment.getExternalStorageDirectory().getPath();
            Utility.WriteLog("3. " + strSDCardPath);
        }
        // ** end replacement code for checking storage directory **

        sdDir = new File(strSDCardPath);
        if (sdDir.canRead())
            Utility.WriteLog("- sdDir.canRead() = true");
        if (sdDir.exists())
            Utility.WriteLog("- sdDir.exists() = true");

        if (sdDir.exists() && sdDir.canRead()) {
            Utility.WriteLog("4.");
            String flashCard = sdDir.getPath();
            String tryFull1 = flashCard + "/qsp/games";
            String tryFull2 = tryFull1 + "/";
            String noMedia = flashCard + "/qsp/.nomedia";
            File f = new File(tryFull1);
            if (f.exists()) {
                CheckNoMedia(noMedia);
                return tryFull2;
            } else {
                if (f.mkdirs()) {
                    CheckNoMedia(noMedia);
                    return tryFull2;
                } else
                    return strSDCardPath;
            }
        }

        return null;
    }

    public static String GetGamesPath(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String path = settings.getString("compGamePath", null);
Utility.WriteLog("GetGamesPath() - [path == null] is "+(path==null));
        return (path != null && !TextUtils.isEmpty(path)) ? path : GetDefaultPath(context);
    }

    public static String GetDownloadPath(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String path = settings.getString("downDirPath", null);
        return (path != null && !TextUtils.isEmpty(path)) ? path : "";
    }

    public static void WriteLog(String msg) {
        String logMsg = "";
        boolean firstMessage = true;
        do {
            if (msg.length() > 4000) {
                logMsg = msg.substring(0, 4000);
                msg = msg.substring(4000);
            }
            else {
                logMsg = msg;
                msg = "";
            }
            if (firstMessage) {
                Log.i("QSP", logMsg);
                firstMessage = false;
            } else Log.i("(cont)", logMsg);

        } while (!msg.equals(""));
    }

    public static void ShowError(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.errorTitle)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    public static void ShowInfo(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void DeleteRecursive(File f) {
        if ((f == null) || !f.exists())
            return;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (File currentFile : files)
                DeleteRecursive(currentFile);
        }
        f.delete();
    }

    public static void DeleteDocFileRecursive (DocumentFile df) {
        if (!df.exists()) return;
        if (df.isDirectory()) {
            DocumentFile[] docFiles = df.listFiles();
            //if there are files in the directory, delete them first
            if (docFiles != null)
                for (DocumentFile currentDocFile : docFiles)
                    DeleteDocFileRecursive(currentDocFile);
        }
        df.delete();
    }

    static final String DEBUGKEY =
            "3082030d308201f5a003020102020477110239300d06092a864886f70d01010b05003037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f6964204465627567301e170d3131303933303038353133325a170d3431303932323038353133325a3037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f696420446562756730820122300d06092a864886f70d01010105000382010f003082010a0282010100f6f9003587afdadb16c3e929e5f9eb61becb48b982d7481b74e997539f3e0a64e36098b34d8b03d9ac1ad36f947c8d3de341a484393ae2e1164b1bed736b73f700e0019831cb1b889f17361694e46c73ecaffcde8dec93fc2ab0bc905947602286e8251971f4205345d3d386cc8ea5cc8bceec248ba7d947728375604d981c76ad69edf020f683a7898d6312df58948a351376c8f5ce030f2b9f8a520445840145647ee9121f41bff315d7ea7d314992356e5d01eefbe16de2d4fb1f978d8df06f148a4b4848cc9a6f63d79291cadaa201eaf3b80a1501184e99a94f42fc5915e7149866124eda49fde4b41b9865fd1b0d7efeb4959b8c84b7b61041e378f7450203010001a321301f301d0603551d0e04160414a08e0f7ad0c9295a95d6011696065cdd09af55fa300d06092a864886f70d01010b050003820101005c335d7f65db96806ac34517eaaa2d7df5a4f273090a7f3881e213e91535790e0e957b66bcdebeab7d23b8198160b2e8d06736d3c57ca807a39e67c58719ae5f8570bed452b71bffe0c3b491bc0cce35e81ed8951c028e6b7296345337b4285dc383a77938c6611f27f67f338c9a0f6355af6e158bb40b328a81c9a22fbd3b7a4e48a57a93f8350712935d88b295dee9523c25a9d3cb819bfff953de5d348d8ec0a6ff861a6bcaf1df65eb168d02ffc88a797132c203ccf06195a6c783f71b757d929dc30c989701b10488182a6ceb39ed89d6988ecc3f714a57df6d0e08e9e4ac706ca1f43edaaff3b1a71edfcd260d0db1a4416af7b1d79459d4028131c525";

    public static boolean signedWithDebugKey(Context context, Class<?> cls) {
        boolean result = false;
        try {
            ComponentName comp = new ComponentName(context, cls);
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), PackageManager.GET_SIGNATURES);
            Signature sigs[] = pinfo.signatures;
            for (int i = 0; i < sigs.length; i++)
                WriteLog(sigs[i].toCharsString());
            if (DEBUGKEY.equals(sigs[0].toCharsString())) {
                result = true;
                WriteLog("package has been signed with the debug key");
            } else {
                WriteLog("package signed with a key other than the debug key");
            }
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return false;
        }
        return result;
    }

    //decodes image and scales it to reduce memory consumption 
    //(пока что не используется)
    public static Bitmap decodeImageFile(File f) {
        try {
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            //The new size we want to scale to
            final int REQUIRED_SIZE = 70;

            //Find the correct scale value. It should be the power of 2.
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
        }
        return null;
    }

    /*
    * @return boolean return true if the application can access the internet
    */
    public static boolean haveInternet(Context context) {
        NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            return false;
        }
        return true;
    }

    //Remove all non-alphanumeric characters ("_" acceptable) from String for use as a file name
    public static String safetyString(String target) {
        if (isNullOrEmpty(target)) return "GameX";

        int i = 0;
        String newStr = target;

        if ( newStr.contains("/") && (i < newStr.length()) )
            do {
                i = newStr.substring(i).indexOf("/")+1;
                newStr = newStr.substring(i);
            } while ( newStr.contains("/") && (i < newStr.length()) );

        //Make the string alphanumeric except for "_" and "."
        newStr = newStr.replaceAll("[^a-zA-Z0-9_.]","");
        //Remove the ".qsp" extension at the end
        newStr = newStr.replace(".qsp","");

        if (newStr == "") {
            newStr = "GameX" + target.length();
            Utility.WriteLog("\""+ target + "\" could not be parsed. Using \""+newStr+"\" for save file prefix.");
        }

        return newStr;
    }

    //Sort an ArrayList of GameItem by title (what is seen on the GameStock menu)
    public static void GameSorter (ArrayList<GameItem> items) {
        //Skip if there aren't at least two files
        if (items.size() < 2) return;

        Collections.sort(items, new Comparator<GameItem>() {
            public int compare(GameItem f1, GameItem f2) {
                return f1.title.toLowerCase().compareTo(f2.title.toLowerCase());
            }
        });
    }


    //Sort an ArrayList of Files by file name
    public static ArrayList<File> FileSorter (ArrayList<File> files) {
        //Skip if there aren't at least two files
        if (files.size() < 2) return files;

        File[] fileArray = files.toArray(new File[files.size()]);
        Arrays.sort(fileArray, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
            }
        });

        ArrayList<File> returnedList = new ArrayList<File>();

        for(int i=0; i<fileArray.length; i++)
            returnedList.add(fileArray[i]);

        return returnedList;
    }

    public static void FileSorter(List<File> files) {
        if (files.size() < 2) return;

        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File first, File second) {
                return first.getName().toLowerCase().compareTo(second.getName().toLowerCase());
            }
        });
    }

    //Compare the file names of two file lists to see if they are the same
    public static boolean directoriesAreEqual(File fileList1[], File fileList2[]) {
        if ((fileList1 == null) || (fileList2 == null)) {
            Utility.WriteLog("Null directory detected!");
            return false;
        }

        if (fileList1.length == fileList2.length) {
            for (int i=0; i<fileList1.length; i++) {
                Utility.WriteLog(fileList1[i].getName()+" vs. "+fileList2[i].getName());
                if (!fileList1[i].getName().equals(fileList2[i].getName())) {
                    Utility.WriteLog("Directories are slightly different!");
                    return false;
                }
            }
            Utility.WriteLog("Directories are the same!");
            return true;
        }
        Utility.WriteLog("Directories are totally different!");
        return false;
    }
}
