package model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Result implements Serializable {
    private Map<String, DocumentData> documentToDocumentDataMap = new HashMap<>();;

    public void addDocumentData(String documentName, DocumentData documentData){
        documentToDocumentDataMap.put(documentName, documentData);
    }

    public Map<String, DocumentData> getDocumentToDocumentDataMap(){
        return Collections.unmodifiableMap(documentToDocumentDataMap);
    }

}
