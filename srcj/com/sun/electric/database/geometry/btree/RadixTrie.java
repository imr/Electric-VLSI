/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RadixTrie.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.database.geometry.btree;

import java.io.*;
import java.util.*;

/**
 *  A <a href=http://en.wikipedia.org/wiki/Radix_trie>Radix Trie</a>;
 *  used to store lists of hierarchically-named signals and
 *  efficiently detect the separator character.
 *
 *  A RadixTrie consists of a tree of nodes; each path from the root
 *  to a node with entry!=null constitutes an entry.  The
 *  concatenation of all TreeMap keys along the path is the key and
 *  the entry value is the entry.  So you might think of a RadixTrie
 *  as a Map<String,V> which has excellent space efficiency when the
 *  keys share large prefixes.
 *
 *  An additional feature in this implementation (not normally
 *  included in RadixTries) is the tracking of each character's
 *  "score".  The score of a branch is defined to be the number of
 *  descendents it has with entry!=null.  The score of a node is the
 *  *second largest* score among its branches.  The score of a
 *  character "c" is the sum of the scores of all nodes "n" such that
 *  the last charachter of its key (in its parent's TreeMap) is "c".
 *  The character with the highest score is a very good guess at the
 *  "hierarchy separator"; it is the character which is most likely to
 *  terminate the longest common prefix of a family of strings.
 *
 *  @author Adam Megacz <adam.megacz@oracle.com>
 */
public class RadixTrie<V> {

    // the root node
    private final Node root = new Node((char)0, null);

    private final int[] scores = new int[65535];

    private class Node {

        private char firstChar;
        private V entry;

        private TreeMap<String,Node> children = null;

        public Node(char firstChar, V value) {
            this.firstChar = firstChar;
            this.entry = value;
        }

        private String getPrev(String key) {
            if (children==null) return null;
            SortedMap<String,Node> prevMap = children.headMap(key);
            return prevMap.size()==0 ? null : prevMap.lastKey();
        }

        public void put(String key, V value) {
            if (key.length()!=0 && children==null) children = new TreeMap<String,Node>();

            // prev is the key right before the "insertion point"
            String prev = getPrev(key);

            if (key.length()==0) {
                this.entry = value;

            } else if (children.containsKey(key)) {
                children.get(key).put("", value);

            } else if (prev!=null && key.startsWith(prev)) {
                // if prev is a prefix of the key we're inserting, we can just walk down that branch
                children.get(prev).put(key.substring(prev.length()), value);
                    
            } else if (prev==null || prev.charAt(0) != key.charAt(0)) {
                // if prev shares not even a single character, we need to make a new branch
                if (children==null) children = new TreeMap<String,Node>();
                children.put(key, new Node(key.charAt(0), value));
                scores[firstChar & 0xffff]++;

            } else {
                // prev shares more than one but less than all characters; need to split a branch
                int i=0;
                for(i=0; i<Math.min(key.length(), prev.length()); i++)
                    if (key.charAt(i)!=prev.charAt(i))
                        break;

                Node oldnode = children.get(prev);
                children.remove(prev);
                scores[firstChar & 0xffff]--;

                Node newnode = new Node(key.charAt(i-1), null);
                children.put(key.substring(0, i), newnode);
                scores[firstChar & 0xffff]++;

                if (newnode.children==null) newnode.children = new TreeMap<String,Node>();
                newnode.children.put(key.substring(i), new Node(key.charAt(key.length()-1), value));
                scores[newnode.firstChar & 0xffff]++;
                newnode.children.put(prev.substring(i), oldnode);
                scores[newnode.firstChar & 0xffff]++;
            }
        }
        private int size() { return children==null ? 0 : children.size(); }
        public V get(String key) {
            if (key.length()==0) return entry;
            if (children==null) return null;
            if (children.containsKey(key)) return children.get(key).get("");
            SortedMap<String,Node> prevMap = children.headMap(key);
            String prev = prevMap.size()==0 ? null : prevMap.lastKey();
            if (prev!=null && key.startsWith(prev)) return children.get(prev).get(key.substring(prev.length()));
            return null;
        }

    }


    public char guessHierarchySeparator() {
        int best = 0;
        char ret = 0;
        for(int i=0; i<scores.length; i++) {
            if (scores[i] > best) {
                best = scores[i];
                ret = (char)i;
            }
        }
        return ret;
    }

    public V    get(String key) {
        return root.get(key);
    }

    public void put(String key, V value) {
        System.out.println("put \""+key+"\"");
        if (value==null) throw new RuntimeException("deletions not yet implemented");
        root.put(key, value);
    }


}
