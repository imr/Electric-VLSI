/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AttributesTable.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;

import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

/**
 * Class to define the Attributes panel in other dialogs.
 */
public class AttributesTable extends JTable implements DatabaseChangeListener {


	/**
	 * Class to define attributes on nodes or arcs.
	 */
	public static class AttValPair
	{
		Variable.Key key;
		String trueName;
		String value;
		String eval;
		boolean code;
	};

	/**
	 * Model for storing Table data
     */
    private static class VariableTableModel extends AbstractTableModel {

        private List<VarEntry> vars;                     // list of variables to display
//        private boolean DEBUG = false;                      // if true, displays database var names
        private static final String [] columnNames = { "Name", "Value", "Code", "Display", "Units" };
        private boolean showCode = true;
        private boolean showDispPos = false;
        private boolean showUnits = false;
        private List<VarEntry> varsToDelete;

        private static class VarEntry {
            // current state of var entry
            private String varTrueName;
            private Variable.Key varKey;
            private Object value;
            private TextDescriptor.Code code;
            private TextDescriptor.DispPos dispPos;
            private TextDescriptor.Unit units;
            private boolean display;
            private ElectricObject owner;
            // if var is null, this means create a new var
            // if any of the above values are different from the original (below) var's value, modify
            // if this is in the varsToDelete list, delete it
            private Variable var;
            // initial state of var entry
            private String initialVarTrueName;
            private Object initialValue;
            private TextDescriptor.Code initialCode;
            private TextDescriptor.DispPos initialDispPos;
            private TextDescriptor.Unit initialUnits;
            private boolean initialDisplay;

            private VarEntry(ElectricObject owner, Variable var) {
                this.owner = owner;
                this.var = var;
                if (var == null) return;

                varKey = var.getKey();
                varTrueName = initialVarTrueName = var.getTrueName();
                value = initialValue = var.getObject();
                code = initialCode = var.getCode();
                dispPos = initialDispPos = var.getDispPart();
                units = initialUnits = var.getUnit();
                display = initialDisplay = var.isDisplay();
            }

            private String getName() { return varTrueName; }
            private Object getObject() { return value; }
            private TextDescriptor.Code getCode() { return code; }
            private TextDescriptor.DispPos getDispPos() { return dispPos; }
            private TextDescriptor.Unit getUnits() { return units; }
            private boolean isDisplay() { return display; }
            private ElectricObject getOwner() { return owner; }
            private Variable.Key getKey() { return varKey; }

            private boolean isChanged() {
                if (!varTrueName.equals(initialVarTrueName)) return true;
                if (value != initialValue) return true;
                if (code != initialCode) return true;
                if (display != initialDisplay) return true;
                if (dispPos != initialDispPos) return true;
                if (units != initialUnits) return true;
                return false;
            }
        }

        /**
         * Class to sort Variables by name.
         */
        public static class VarEntrySort implements Comparator<VarEntry>
        {
            public int compare(VarEntry v1, VarEntry v2)
            {
                String s1 = v1.getName();
                String s2 = v2.getName();
                return s1.compareToIgnoreCase(s2);
            }
        }

        // constructor
        private VariableTableModel(boolean showCode, boolean showDispPos, boolean showUnits) {
            vars = new ArrayList<VarEntry>();
            varsToDelete = new ArrayList<VarEntry>();
            this.showCode = showCode;
            this.showDispPos = showDispPos;
            this.showUnits = showUnits;
        }

        /** Get number of columns in table */
        public int getColumnCount() {
            int c = 2;
            if (showCode) c++;
            if (showDispPos) c++;
            if (showUnits) c++;
            return c;
        }

        /** Get number of rows in table */
        public int getRowCount() {
            return vars.size();
        }

        private int getCodeColumn() {
            if (!showCode) return -1;
            else return 2;
        }

        private int getDispColumn() {
            if (!showDispPos) return -1;
            int c = 2;
            if (showCode) c++;
            return c;
        }

        private int getUnitsColumn() {
            if (!showUnits) return -1;
            int c = 2;
            if (showCode) c++;
            if (showDispPos) c++;
            return c;
        }

        /** Get object at location in table */
        public Object getValueAt(int rowIndex, int columnIndex) {

            // order: name, value
            VarEntry ve = vars.get(rowIndex);

            if (ve == null) return null;

            if (columnIndex == 0) {
                // name
                return ve.getName();
            }
            if (columnIndex == 1) {
                // value
                return ve.getObject().toString();
            }
            if (columnIndex == getCodeColumn()) {
                return ve.getCode();
            }
            if (columnIndex == getDispColumn()) {
                if (!ve.isDisplay()) return displaynone;
                return ve.getDispPos();
            }
            if (columnIndex == getUnitsColumn()) {
                return ve.getUnits();
            }
            return null;
        }

        /** Get column header names */
        public String getColumnName(int col) {
            if (col < 2)
                return columnNames[col];
            if (col == getCodeColumn()) return columnNames[2];
            if (col == getDispColumn()) return columnNames[3];
            if (col == getUnitsColumn()) return columnNames[4];
            return null;
        }

        /** See if cell is editable. */
        public boolean isCellEditable(int row, int col) {
            if (col > 0) return true;
            return false;
        }

        /** Set a value */
        public void setValueAt(Object aValue, int row, int col) {
            VarEntry ve = vars.get(row);
            ElectricObject owner = ve.getOwner();
            if (ve == null) return;
            if (owner == null) return;

            if (col == 0) return;                   // can't change var name

            if (col == 1) {
                if (!aValue.toString().equals(ve.getObject().toString())) {
                    ve.value = aValue;
                    fireTableCellUpdated(row, col);
                }
                return;
            }

            if (col == getCodeColumn()) {
                TextDescriptor.Code newCode = (TextDescriptor.Code)aValue;
                if (newCode != ve.getCode()) {
                    ve.code = newCode;
                    fireTableCellUpdated(row, col);
                }
                return;
            }

            if (col == getDispColumn()) {
                if (aValue == displaynone) {
                    if (ve.isDisplay()) {
                        ve.display = false;
                        fireTableCellUpdated(row, col);
                    }
                } else {
                    TextDescriptor.DispPos newDispPos = (TextDescriptor.DispPos)aValue;
                    if ((newDispPos != ve.getDispPos()) || !ve.isDisplay()) {
                        ve.dispPos = newDispPos;
                        ve.display = true;
                        fireTableCellUpdated(row, col);
                    }
                }
                return;
            }

            if (col == getUnitsColumn()) {
                TextDescriptor.Unit newUnit = (TextDescriptor.Unit)aValue;
                if (newUnit != ve.getUnits()) {
                    ve.units = newUnit;
                    fireTableCellUpdated(row, col);
                }
                return;
            }
        }

        /**
         * Set this to be the list of variables shown
         * @param owner ElectricObject which owns Variables 
         * @param variables the list of Variables to show
         */
        private void setVars(ElectricObject owner, List<Variable> variables) {
            vars.clear();
            varsToDelete.clear();
            // sort by name
            for (Variable var : variables) {
                vars.add(new VarEntry(owner, var));
            }
            Collections.sort(vars, new VarEntrySort());
            fireTableDataChanged();
        }

        /** Add a variable to be displayed in the Table */
//        private void addVariable(Variable var) {
//            vars.add(new VarEntry(var));
//            Collections.sort(vars, new VarEntrySort());
//            fireTableDataChanged();
//        }

        private void clearVariables() {
            vars.clear();
            varsToDelete.clear();
            fireTableDataChanged();
        }

        public Class<?> getColumnClass(int col) {
            if (col == getCodeColumn()) return TextDescriptor.Code.class;
            if (col == getDispColumn()) return Object.class;
            if (col == getUnitsColumn()) return TextDescriptor.Unit.class;
            return String.class;
        }

        public void setShowCode(boolean showCode) {
            if (this.showCode == showCode) return;
            this.showCode = showCode;
            fireTableStructureChanged();
        }

        public void setShowDisp(boolean showDisp) {
            if (this.showDispPos == showDisp) return;
            this.showDispPos = showDisp;
            fireTableStructureChanged();
        }

        public void setShowUnits(boolean showUnits) {
            if (this.showUnits == showUnits) return;
            this.showUnits = showUnits;
            fireTableStructureChanged();
        }

        /**
         * Create a new var with default properties
         */
        public void newVar(ElectricObject owner) {
            VarEntry ve = new VarEntry(owner, null);
            ve.var = null;
            ve.varKey = null;
            ve.varTrueName = getUniqueName("newVar");
            ve.value = "?";
            ve.code = TextDescriptor.Code.NONE;
            ve.dispPos = TextDescriptor.DispPos.NAMEVALUE;
            ve.units = TextDescriptor.Unit.NONE;
            ve.display = true;

            vars.add(ve);
            Collections.sort(vars, new VarEntrySort());
            fireTableDataChanged();
        }

        /**
         * Duplicate the variable in the specified row
         * @param row the row containing the var to duplicate
         */
        public void duplicateVar(int row) {
            if (row >= vars.size()) {
                JOptionPane.showMessageDialog(null, "Please select an attribute to duplicate",
                        "Invalid Action", JOptionPane.WARNING_MESSAGE);
                return;
            }
            VarEntry srcVe = vars.get(row);

            VarEntry ve = new VarEntry(srcVe.getOwner(), null);
            ve.var = null;
            ve.varKey = null;
            ve.varTrueName = getUniqueName(srcVe.getName());
            ve.value = srcVe.getObject();
            ve.code = srcVe.getCode();
            ve.dispPos = srcVe.getDispPos();
            ve.units = srcVe.getUnits();
            ve.display = srcVe.isDisplay();

            vars.add(ve);
            Collections.sort(vars, new VarEntrySort());
            fireTableDataChanged();
        }

        /**
         * Delete the var in the specified row
         * @param row the row containing the var to delete
         */
        public void deleteVar(int row) {
            if (row >= vars.size()) {
                JOptionPane.showMessageDialog(null, "Please select an attribute to delete",
                        "Invalid Action", JOptionPane.WARNING_MESSAGE);
                return;
            }
            VarEntry ve = (VarEntry)vars.remove(row);
            varsToDelete.add(ve);
            fireTableDataChanged();
        }

        /**
         * Apply all new/delete/duplicate/modify changes
         */
        public void applyChanges()
        {
        	// prepare information about deleted attributes
        	List<Variable.Key> deleteTheseVars = new ArrayList<Variable.Key>();
        	ElectricObject owner = null;
            for (VarEntry ve : varsToDelete)
            {
                Variable var = ve.var;
                if (var == null) continue;
                owner = ve.getOwner();
                deleteTheseVars.add(var.getKey());
            }

            // prepare information about new and modified attributes
        	List<Variable.Key> createKey = new ArrayList<Variable.Key>();
        	List<Object> createValue = new ArrayList<Object>();
        	List<Boolean> createNew = new ArrayList<Boolean>();
        	List<Boolean> createDisplay = new ArrayList<Boolean>();
        	List<Integer> createCode = new ArrayList<Integer>();
        	List<Integer> createDispPos = new ArrayList<Integer>();
        	List<Integer> createUnits = new ArrayList<Integer>();

            for(VarEntry ve : vars)
            {
                Variable var = ve.var;
                owner = ve.getOwner();
                Variable.Key newKey = null;
                Object newValue = ve.getObject();
                boolean newCreate = false;
                boolean newDisplay = ve.isDisplay();
                int newCode = ve.getCode().getCFlags();
                int newDispPos = ve.getDispPos().getIndex();
                int newUnits = ve.getUnits().getIndex();

                if (var == null)
                {
                    // this is a new var
                	newCreate = true;
                    String name = ve.getName();
                    if (!name.startsWith("ATTR_") && !name.startsWith("ATTRP_"))
                        name = "ATTR_"+name;
                    newKey = Variable.newKey(name);
                } else
                {
                    if (!ve.isChanged()) continue;
                    newKey = ve.getKey();
                }
            	createKey.add(newKey);
            	createValue.add(newValue);
            	createNew.add(new Boolean(newCreate));
            	createDisplay.add(new Boolean(newDisplay));
            	createCode.add(new Integer(newCode));
            	createDispPos.add(new Integer(newDispPos));
            	createUnits.add(new Integer(newUnits));
            }
            new ApplyChanges(owner, createKey, createValue, createNew, createDisplay, createCode, createDispPos, createUnits, deleteTheseVars);
        }

        /**
         * Cancel all changes
         */
        public void cancelChanges() {

        }

        private String getUniqueName(String name) {
            boolean nameConflict = true;
            String newName = name;
            int i = 0;
            while (nameConflict) {
                nameConflict = false;
                i++;
                for (VarEntry ve : vars) {
                    if (newName.equals(ve.getName())) {
                        nameConflict = true;
                        newName = name + "_" + i;
                        break;
                    }
                }
            }
            return newName;
        }

        private static class ApplyChanges extends Job
        {
        	private ElectricObject owner;
            private List<Variable.Key> createKey;
            private List<Object> createValue;
            private List<Boolean> createNew;
            private List<Boolean> createDisplay;
            private List<Integer> createCode;
            private List<Integer> createDispPos;
            private List<Integer> createUnits;
            private List<Variable.Key> varsToDelete;

            private ApplyChanges(ElectricObject owner,
            	List<Variable.Key> createKey,
	            List<Object> createValue,
	            List<Boolean> createNew,
	            List<Boolean> createDisplay,
	            List<Integer> createCode,
	            List<Integer> createDispPos,
	            List<Integer> createUnits,
	            List<Variable.Key> varsToDelete)
            {
                super("Apply Attribute Changes", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
                this.owner = owner;
                this.createKey = createKey;
                this.createValue = createValue;
                this.createNew = createNew;
                this.createDisplay = createDisplay;
                this.createCode = createCode;
                this.createDispPos = createDispPos;
                this.createUnits = createUnits;
                this.varsToDelete = varsToDelete;
                startJob();
            }

            public boolean doIt() throws JobException
            {
                // delete variables first
                for (Variable.Key key : varsToDelete)
                {
                    owner.delVar(key);
                }

                // now create new and update existing variables
                for(int i=0; i<createKey.size(); i++)
                {
                	Variable.Key key = createKey.get(i);
                	Object obj = createValue.get(i);
                	boolean makeNew = createNew.get(i).booleanValue();
                	boolean display = createDisplay.get(i).booleanValue();
                	TextDescriptor.Code code = TextDescriptor.Code.getByCBits(createCode.get(i).intValue());
                	TextDescriptor.DispPos dispPos = TextDescriptor.DispPos.getShowStylesAt(createDispPos.get(i).intValue());
                	TextDescriptor.Unit units = TextDescriptor.Unit.getUnitAt(createUnits.get(i).intValue());

                    Variable newVar = null;
                    if (makeNew)
                    {
                        // this is a new variable
                        newVar = owner.newVar(key, obj);
//                        ve.var = newVar;
                    } else
                    {
                        // update variable
                        newVar = owner.updateVar(key, obj);
//                        ve.var = newVar;
                    }

                    if (newVar != null)
                    {
                        // set/update properties
                        TextDescriptor td = newVar.getTextDescriptor();
                        td = td.withDisplay(display).withCode(code).withDispPart(dispPos).withUnit(units);
                        owner.setTextDescriptor(newVar.getKey(), td);
                    }
                }
                return true;
            }

        } // end class ApplyChanges

    } // end class VariableTableModel

    // ----------------------------------------------------------------

    private static JComboBox codeComboBox = null;
    private static JComboBox dispComboBox = null;
    private static JComboBox unitComboBox = null;
    private static final String displaynone = "None";
    private JPopupMenu popup;
    private Point popupLocation;
    private ElectricObject owner;
    private boolean showAttrOnly;

    /**
     * Create a new Attributes Table
     */
    public AttributesTable(ElectricObject owner, boolean showCode, boolean showDispPos, boolean showUnits) {
        setElectricObject(owner);
        showAttrOnly = Job.getDebug();

        setGridColor(getBackground());          // hides cell grids
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        VariableTableModel model = new VariableTableModel(showCode, showDispPos, showUnits);
        setModel(model);

        initComboBoxes();

        // set up combo box editors
        if (showCode) {
            TableColumn codeColumn = getColumnModel().getColumn((model.getCodeColumn()));
            if (codeColumn != null)
                codeColumn.setCellEditor(new DefaultCellEditor(codeComboBox));
        }
        if (showDispPos) {
            TableColumn codeColumn = getColumnModel().getColumn((model.getDispColumn()));
            if (codeColumn != null)
                codeColumn.setCellEditor(new DefaultCellEditor(dispComboBox));
        }
        if (showUnits){
            TableColumn codeColumn = getColumnModel().getColumn((model.getUnitsColumn()));
            if (codeColumn != null)
                codeColumn.setCellEditor(new DefaultCellEditor(unitComboBox));
        }

        MouseListener mouseListener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isShiftDown() || e.isControlDown() || e.isAltDown()) return;

                if (e.isMetaDown()) {
                    initPopupMenu();
                    popupLocation = new Point(e.getX(), e.getY());
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };
        addMouseListener(mouseListener);
        UserInterfaceMain.addDatabaseChangeListener(this);
    }

    private void initComboBoxes() {
        if (codeComboBox == null) {
            codeComboBox = new JComboBox();
            for (Iterator<TextDescriptor.Code> it = TextDescriptor.Code.getCodes(); it.hasNext(); ) {
                codeComboBox.addItem(it.next());
            }
            codeComboBox.setFont(new Font("Dialog", 0, 11));
        }
        if (dispComboBox == null) {
            dispComboBox = new JComboBox();
            dispComboBox.addItem(displaynone);
            for (Iterator<TextDescriptor.DispPos> it = TextDescriptor.DispPos.getShowStyles(); it.hasNext(); ) {
                dispComboBox.addItem(it.next());
            }
            dispComboBox.setFont(new Font("Dialog", 0, 11));
        }
        if (unitComboBox == null) {
            unitComboBox = new JComboBox();
            for (Iterator<TextDescriptor.Unit> it = TextDescriptor.Unit.getUnits(); it.hasNext(); ) {
                unitComboBox.addItem(it.next());
            }
            unitComboBox.setFont(new Font("Dialog", 0, 11));
        }
    }

    private void initPopupMenu() {
        popup = new JPopupMenu();
        JMenuItem m;
        m = new JMenuItem("New Attr");
        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { newVar(); }
        });
        popup.add(m);
/*        m = new JMenuItem("Edit Cell");
        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { editCell(popup.getLocation()); }
        });
        popup.add(m);*/
        m = new JMenuItem("Duplicate Attr");
        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { duplicateVar(popupLocation); }
        });
        popup.add(m);
        m = new JMenuItem("Delete Attr");
        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { deleteVar(popupLocation); }
        });
        popup.add(m);
        JMenu showMenu = new JMenu("Show...");
        JCheckBoxMenuItem cb;
        VariableTableModel model = (VariableTableModel)getModel();
        cb = new JCheckBoxMenuItem("Code", model.showCode);
        cb.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) { toggleShowCode(); }
        });
        showMenu.add(cb);
        cb = new JCheckBoxMenuItem("Display", model.showDispPos);
        cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { toggleShowDisp(); }
        });
        showMenu.add(cb);
        cb = new JCheckBoxMenuItem("Units", model.showUnits);
        cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { toggleShowUnits(); }
        });
        showMenu.add(cb);
        popup.add(showMenu);
    }

    /**
     * Create a new Variable
     */
    private void newVar() {
        VariableTableModel model = (VariableTableModel)getModel();
        model.newVar(owner);
    }

    /**
     * Duplicate the Variable in the row pointed to by location
     * @param location
     */
    private void duplicateVar(Point location) {
        int row = rowAtPoint(location);
        VariableTableModel model = (VariableTableModel)getModel();
        model.duplicateVar(row);
    }

    /**
     * Delete the Variable in the row pointed to by location
     * @param location
     */
    private void deleteVar(Point location) {
        int row = rowAtPoint(location);
        VariableTableModel model = (VariableTableModel)getModel();
        model.deleteVar(row);
    }

    /**
     * Applies all changes made to attributes to database
     */
    public void applyChanges() {
        VariableTableModel model = (VariableTableModel)getModel();
        // clean up if a cell is currently being edited
        if (isEditing()) {
            int row = getEditingRow();
            int col = getEditingColumn();
            TableCellEditor editor = getCellEditor(row, col);
            editor.stopCellEditing();
        }
        model.applyChanges();
    }

	/**
	 * Method to handle the "Cancel" button in attributes.
	 */
    public void cancelChanges() {
        VariableTableModel model = (VariableTableModel)getModel();
        // clean up if a cell is currently being edited
        if (isEditing()) {
            int row = getEditingRow();
            int col = getEditingColumn();
            TableCellEditor editor = getCellEditor(row, col);
            editor.cancelCellEditing();
        }
        // revert back to database values
        setElectricObject(owner);
    }


    private void toggleShowCode() {
        VariableTableModel model = (VariableTableModel)getModel();
        model.setShowCode(!model.showCode);
        updateEditors();
    }

    private void toggleShowDisp() {
        VariableTableModel model = (VariableTableModel)getModel();
        model.setShowDisp(!model.showDispPos);
        updateEditors();
    }

    private void toggleShowUnits() {
        VariableTableModel model = (VariableTableModel)getModel();
        model.setShowUnits(!model.showUnits);
        updateEditors();
    }

    private void updateEditors() {
        VariableTableModel model = (VariableTableModel)getModel();
        int codeCol = model.getCodeColumn();
        if (codeCol != -1) {
            TableColumn codeColumn = getColumnModel().getColumn(codeCol);
            if (codeColumn != null)
                codeColumn.setCellEditor(new DefaultCellEditor(codeComboBox));
        }
        int dispCol = model.getDispColumn();
        if (dispCol != -1) {
            TableColumn codeColumn = getColumnModel().getColumn(dispCol);
            if (codeColumn != null)
                codeColumn.setCellEditor(new DefaultCellEditor(dispComboBox));
        }
        int unitsCol = model.getUnitsColumn();
        if (unitsCol != -1) {
            TableColumn codeColumn = getColumnModel().getColumn(unitsCol);
            if (codeColumn != null)
                codeColumn.setCellEditor(new DefaultCellEditor(unitComboBox));
        }
    }

    /**
     * Set the ElectricObject whose Variables will be shown
     * @param eobj
     */
    public void setElectricObject(ElectricObject eobj) {
        if (owner == eobj) return;
        // clear old vars
        clearVariables();
        // add new vars
        if (eobj != null) {
            List<Variable> vars = new ArrayList<Variable>();
            for (Iterator<Variable> it = eobj.getVariables(); it.hasNext(); ) {
                // only add attributes
                Variable var = it.next();
                if (var.isAttribute())
                    vars.add(var);
            }
            // sort vars by name
            //Collections.sort(vars, new Attributes.VariableNameSort());
            ((VariableTableModel)getModel()).setVars(eobj, vars);
        }
        owner = eobj;
    }

    /**
     * Clear all variables from the table
     */
    private void clearVariables() {
        ((VariableTableModel)getModel()).clearVariables();
    }

//     public void databaseChanged(Undo.Change evt) {}
//     public boolean isGUIListener() { return true; }
//     public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
//         // reload vars
//         ElectricObject eobj = owner;
//         setElectricObject(null);
//         setElectricObject(eobj);
//     }

    public void databaseChanged(DatabaseChangeEvent e) {
        // reload vars
        ElectricObject eobj = owner;
        setElectricObject(null);
        setElectricObject(eobj);
    }

}
