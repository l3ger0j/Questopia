package org.qp.android.dto.stock;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "game")
public class RemoteGameData {

    @JacksonXmlCData
    public long id;

    @JacksonXmlProperty(localName = "list_id")
    @JacksonXmlCData
    public String listId = "";

    @JacksonXmlCData
    public String author = "";

    @JacksonXmlProperty(localName = "ported_by")
    @JacksonXmlCData
    public String portedBy = "";

    @JacksonXmlCData
    public String version = "";

    @JacksonXmlCData
    public String title = "";

    @JacksonXmlCData
    public String lang = "";

    @JacksonXmlCData
    public String player = "";

    @JacksonXmlCData
    public String icon = "";

    @JacksonXmlProperty(localName = "file_url")
    @JacksonXmlCData
    public String fileUrl = "";

    @JacksonXmlProperty(localName = "file_size")
    @JacksonXmlCData
    public long fileSize;

    @JacksonXmlProperty(localName = "file_ext")
    @JacksonXmlCData
    public String fileExt = "";

    @JacksonXmlProperty(localName = "desc_url")
    @JacksonXmlCData
    public String descUrl = "";

    @JacksonXmlProperty(localName = "pub_date")
    @JacksonXmlCData
    public String pubDate = "";

    @JacksonXmlProperty(localName = "mod_date")
    @JacksonXmlCData
    public String modDate = "";

    public RemoteGameData() {}

    @NonNull
    @Override
    public String toString() {
        return "RemoteGameData{" +
                "id='" + id + '\'' +
                ", listId='" + listId + '\'' +
                ", author='" + author + '\'' +
                ", portedBy='" + portedBy + '\'' +
                ", version='" + version + '\'' +
                ", title='" + title + '\'' +
                ", lang='" + lang + '\'' +
                ", player='" + player + '\'' +
                ", fileUrl='" + fileUrl + '\'' +
                ", fileSize='" + fileSize + '\'' +
                ", fileExt='" + fileExt + '\'' +
                ", descUrl='" + descUrl + '\'' +
                ", pubDate='" + pubDate + '\'' +
                ", modDate='" + modDate + '\'' +
                '}';
    }
}
