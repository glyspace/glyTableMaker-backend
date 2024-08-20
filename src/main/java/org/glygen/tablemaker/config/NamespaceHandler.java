package org.glygen.tablemaker.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.glygen.tablemaker.view.NamespaceEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sena Arpinar
 *
 */
public class NamespaceHandler {
    
    final static Logger logger = LoggerFactory.getLogger("event-logger");

    private static String resourceFolderName = "namespaces";
    
    //protected static Map<String, String> namespaceFileMapping = new HashMap<>();
    protected static Map <String, PatriciaTrie<List<NamespaceEntry>>> namespaces = new HashMap<>();
    
    public static void loadNamespaces(){
        // check the folder and find all namespace files, 
        // load them into the namespaces hashmap
        File namespaceFolder = new File (resourceFolderName);
        String[] namespaceFiles = namespaceFolder.list();
        if (namespaceFiles != null) {
        	long startingMemory = Runtime.getRuntime().freeMemory();
            logger.info("Memory before namespaces:" + startingMemory);
	        for (String filename: namespaceFiles) {
	            //String namespace = filename.substring(0, filename.indexOf("_"));
	            //namespaceFileMapping.put(namespace, filename);
	            File namespaceFile = new File(namespaceFolder + File.separator + filename);
	            if(namespaceFile.exists())
	            {
	                logger.info("Creating trie from namespace file : " + namespaceFile.getName());
	                PatriciaTrie<List<NamespaceEntry>> trie = parseNamespaceFile(namespaceFile.getAbsolutePath());
	                namespaces.put (filename, trie);
	            }
	        }
	        
	        long endMemory = Runtime.getRuntime().freeMemory();
	        logger.info("NamespaceHandler memory usage: " +  (endMemory - startingMemory)/1000000 + " MB" );
	        logger.info("Memory after namespaces:" + endMemory);
	    }
    }
        
    /**
     * This is the default implementation which expects a file with two columns separated with a tab (\t) and 
     * each line corresponds to a new entry. First column should contain all the synonyms to be matched and the second 
     * column should have the actual value to be used
     * 
     * NOTE: Subclasses should override this method to parse their specific file formats
     *
     * @param filename containing the synonyms
     * @return a PatriciaTrie for searching
     */
    public static PatriciaTrie<List<NamespaceEntry>> parseNamespaceFile (String filename) {
        PatriciaTrie<List<NamespaceEntry>> trie = new PatriciaTrie<List<NamespaceEntry>>();
        
        long startTime = System.currentTimeMillis();
        InputStream inputStream;
        try {
        	inputStream = new FileInputStream(filename);
            if (filename.endsWith("gz")) {
            	inputStream = new GZIPInputStream(inputStream);
            }
            logger.info("Reading namespaces from inputstream");
            BufferedReader names = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line=names.readLine())!=null) {
                String[] parts = line.split("\\t");
                String synonym = parts[0].trim();
                String name = parts[1].trim();
                String uri = null;
                if (parts.length > 2) {
                	uri = parts[2].trim();
                }
                
                NamespaceEntry entry = new NamespaceEntry();
                entry.setLabel(name);
                entry.setUri(uri);
                
                List<NamespaceEntry> entries = trie.get(synonym.toLowerCase());
                if (entries == null) {
                	entries = new ArrayList<>();
                }
                entries.add(entry);
                
                trie.put (synonym.toLowerCase(), entries); 
            }
            logger.info("Closing inputstream for namespace file");
            inputStream.close();
            names.close();
        } catch (FileNotFoundException e) {
            logger.error("Cannot find the namespace: " + filename, e);
        } catch (IOException e) {
            logger.error("Cannot read the namespace: " + filename, e);
        } catch (Exception | Error e) {
            logger.error("Cannot load the namespace: " + filename + "\n" + e.getMessage(), e);
            throw e;
        }
        logger.info("NamespaceHandler Took: " +  (System.currentTimeMillis() - startTime)/1000.0 + " seconds for file: " + filename);
        return trie;
    }
    
    /**
     * This implementation returns a PatriciaTrie using the strings in the given list as the synonyms and the actual values to show
     * for the type ahead.
     * 
     * @param items list of strings
     * @return patriciaTrie to be used in typeahead implementation
     */
    public static PatriciaTrie<List<NamespaceEntry>> createNamespaceFromList (List<String> items) {
        PatriciaTrie<List<NamespaceEntry>> trie = new PatriciaTrie<List<NamespaceEntry>>();
        for (String item: items) {
        	NamespaceEntry entry = new NamespaceEntry();
        	entry.setLabel(item);
        	List<NamespaceEntry> entries = new ArrayList<>();
        	entries.add(entry);
            trie.put(item.toLowerCase(), entries);
        }
        return trie;
    }
    
    public static PatriciaTrie<List<NamespaceEntry>> getTrieForNamespace(String namespace) {
        return namespaces.get(namespace);
    }
}