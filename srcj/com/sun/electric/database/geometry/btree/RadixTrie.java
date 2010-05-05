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
 *  concatenation of all label fields along the path is the key and
 *  the entry value is the entry.  So you might think of a RadixTrie
 *  as a Map<String,V> which has excellent space efficiency when the
 *  keys share large prefixes.
 *
 *  An additional feature in this implementation (not normally
 *  included in RadixTries) is the tracking of each character's
 *  "score".  The score of a node is defined to be the number of
 *  descendents it has with entry!=null.  The score of a character "c"
 *  is the sum of the scores of all nodes for which
 *  label.charAt(label.length()-1)==c.  The character with the highest
 *  score is a very good guess at the "hierarchy separator"; it is the
 *  character which is most likely to terminate the longest common
 *  prefix of a family of strings.
 *
 *  @author Adam Megacz <adam.megacz@oracle.com>
 */
public class RadixTrie<V> {

    // the root node
    private Node root = null;

    private class Node {
        // if this!=root then label.length()!=0
        String label;

        // invariant: for any character "c", children.get(c).label.charAt(0)==c
        HashMap<Character,Node> children = null;

        V entry;
    }
}