package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.Main;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.event.*;

/**
 * Class to define the Attributes panel in other dialogs.
 */
public class AttributesTable extends JTable implements DatabaseChangeListener {

    /**
     * Model for storing Table data
     * */
    private static class VariableTableModel extends AbstractTableModel {

        private List varInfos;                     // list of variables to display
        private boolean DEBUG = false;                      // if true, displays database var names
        private static final String [] columnNames = { "Name", "Value", "Code", "Display", "Units" };
        private boolean showCode = true;
        private boolean showDispPos = false;
        private boolean showUnits = false;

        // Class to hold var and owner pair
        private static class VarInfo {
            private Variable var;
            private ElectricObject owner;
        }

        // constructor
        private VariableTableModel(boolean showCode, boolean showDispPos, boolean showUnits) {
            varInfos = new ArrayList();
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
            return varInfos.size();
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

        /** Get the variable at the designated row number */
        private Variable getVarAtRow(int row) {
            if (row < 0) return null;
            if (row > (varInfos.size()-1)) return null;
            VarInfo vi = (VarInfo)varInfos.get(row);
            return vi.var;
        }

        /** Get object at location in table */
        public Object getValueAt(int rowIndex, int columnIndex) {

            // order: name, value
            VarInfo vc = (VarInfo)varInfos.get(rowIndex);
            Variable var = vc.var;

            if (var == null) return null;

            if (columnIndex == 0) {
                // name
                if (DEBUG) return var.getKey().getName();
                return var.getTrueName();
            }
            if (columnIndex == 1) {
                // value
                return var.getObject().toString();
            }

            if (columnIndex == getCodeColumn()) {
                return var.getCode();
            }

            if (columnIndex == getDispColumn()) {
                TextDescriptor td = var.getTextDescriptor();
                if (td == null) return null;
                return td.getDispPart();
            }

            if (columnIndex == getUnitsColumn()) {
                TextDescriptor td = var.getTextDescriptor();
                if (td == null) return null;
                return td.getUnit();
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
            VarInfo vc = (VarInfo)varInfos.get(row);
            Variable var = vc.var;
            ElectricObject owner = vc.owner;
            if (var == null) return;
            if (owner == null) return;
            TextDescriptor td = var.getTextDescriptor();

            if (col == 0) return;                   // can't change var name

            if (col == 1) {
                if (!aValue.toString().equals(var.getObject().toString())) {
                    VarChange job = new VarChange(var, owner, var.getCode(), td.getDispPart(), td.getUnit(),
                            aValue, var.isDisplay());
                    //fireTableCellUpdated(row, col);
                }
                return;
            }

            if (col == getCodeColumn()) {
                Variable.Code newCode = (Variable.Code)aValue;
                if (newCode != var.getCode()) {
                    VarChange job = new VarChange(var, owner, newCode, td.getDispPart(), td.getUnit(),
                            var.getObject(), var.isDisplay());
                    //fireTableCellUpdated(row, col);
                }
                return;
            }

            if (col == getDispColumn()) {
                if (aValue == displaynone) {
                    if (var.isDisplay()) {
                        VarChange job = new VarChange(var, owner, var.getCode(), td.getDispPart(), td.getUnit(),
                                var.getObject(), false);
                        //fireTableCellUpdated(row, col);
                    }
                } else {
                    TextDescriptor.DispPos newDispPos = (TextDescriptor.DispPos)aValue;
                    if (newDispPos != td.getDispPart()) {
                        VarChange job = new VarChange(var, owner, var.getCode(), newDispPos, td.getUnit(),
                                var.getObject(), true);
                        //fireTableCellUpdated(row, col);
                    }
                }
                return;
            }

            if (col == getUnitsColumn()) {
                TextDescriptor.Unit newUnit = (TextDescriptor.Unit)aValue;
                if (newUnit != td.getUnit()) {
                    VarChange job = new VarChange(var, owner, var.getCode(), td.getDispPart(), newUnit,
                            var.getObject(), var.isDisplay());
                    //fireTableCellUpdated(row, col);
                }
                return;
            }
        }

        /** Add a variable to be displayed in the Table */
        private void addVariable(Variable var, ElectricObject owner) {
            VarInfo vi = new VarInfo();
            vi.var = var;
            vi.owner = owner;
            varInfos.add(vi);
            fireTableDataChanged();
        }

        private void clearVariables() {
            varInfos.clear();
            fireTableDataChanged();
        }

        public Class getColumnClass(int col) {
            if (col == getCodeColumn()) return Variable.Code.class;
            if (col == getDispColumn()) return TextDescriptor.DispPos.class;
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
    }

    // ----------------------------------------------------------------

    private static JComboBox codeComboBox = null;
    private static JComboBox dispComboBox = null;
    private static JComboBox unitComboBox = null;
    private static final String displaynone = "None";
    private JPopupMenu popup;
    private ElectricObject owner;
    private boolean showAttrOnly;

    /**
     * Create a new Attributes Table
     */
    public AttributesTable(ElectricObject owner, boolean showCode, boolean showDispPos, boolean showUnits) {
        setElectricObject(owner);
        showAttrOnly = Main.getDebug();

        setGridColor(getBackground());          // hides cell grids
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        VariableTableModel model = new VariableTableModel(showCode, showDispPos, showUnits);
        setModel(model);

        initComboBoxes();
        //initPopupMenu();

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
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };
        addMouseListener(mouseListener);
        Undo.addDatabaseChangeListener(this);
    }

    private void initComboBoxes() {
        if (codeComboBox == null) {
            codeComboBox = new JComboBox();
            for (Iterator it = Variable.Code.getCodes(); it.hasNext(); ) {
                codeComboBox.addItem(it.next());
            }
            codeComboBox.setFont(new Font("Dialog", 0, 11));
        }
        if (dispComboBox == null) {
            dispComboBox = new JComboBox();
            dispComboBox.addItem(displaynone);
            for (Iterator it = TextDescriptor.DispPos.getShowStyles(); it.hasNext(); ) {
                dispComboBox.addItem(it.next());
            }
            dispComboBox.setFont(new Font("Dialog", 0, 11));
        }
        if (unitComboBox == null) {
            unitComboBox = new JComboBox();
            for (Iterator it = TextDescriptor.Unit.getUnits(); it.hasNext(); ) {
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
            public void actionPerformed(ActionEvent e) { duplicateVar(popup.getLocation()); }
        });
        popup.add(m);
        m = new JMenuItem("Delete Attr");
        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { deleteVar(popup.getLocation()); }
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
        // create the new var with basic defaults
        newVar("newVar", "?", Variable.Code.NONE, TextDescriptor.DispPos.NAMEVALUE, TextDescriptor.Unit.NONE);
    }

    /**
     * Create a new Variable
     * @param name
     * @param value
     * @param code
     * @param dispPos
     * @param units
     */
    private void newVar(String name, Object value, Variable.Code code,
                        TextDescriptor.DispPos dispPos, TextDescriptor.Unit units) {
        if (owner == null) {
            System.out.println("No object specified on which to create Variable");
            return;
        }
        if (!name.startsWith("ATTR_")) name = "ATTR_" + name;
        CreateAttribute job = new CreateAttribute(name, value, owner, code, dispPos, units);
    }

    /**
     * Edit the cell at location location
     * @param location
     */
    private void editCell(Point location) {
        int row = rowAtPoint(location);
        int col = columnAtPoint(location);
        editCellAt(row, col);
    }

    /**
     * Duplicate the Variable in the row pointed to by location
     * @param location
     */
    private void duplicateVar(Point location) {
        int row = rowAtPoint(location);
        VariableTableModel model = (VariableTableModel)getModel();
        Variable var = model.getVarAtRow(row);
        if (var == null) {
            JOptionPane.showMessageDialog(null, "Please select an attribute to duplicate",
                    "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }
        TextDescriptor td = var.getTextDescriptor();
        newVar(var.getTrueName(), var.getObject(), var.getCode(), td.getDispPart(), td.getUnit());
    }

    /**
     * Delete the Variable in the row pointed to by location
     * @param location
     */
    private void deleteVar(Point location) {
        int row = rowAtPoint(location);
        VariableTableModel model = (VariableTableModel)getModel();
        Variable var = model.getVarAtRow(row);
        if (var == null) {
            JOptionPane.showMessageDialog(null, "Please select an attribute to delete",
                    "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int ret = JOptionPane.showConfirmDialog(null, "Are you sure you really want to delete "+var.getTrueName()+"?",
                "Confirm Attribute Delete", JOptionPane.YES_NO_OPTION);
        if (ret == JOptionPane.YES_OPTION) {
            // delete the var
            DeleteAttribute job = new DeleteAttribute(var, owner);
        }
    }

    private void toggleShowCode() {
        VariableTableModel model = (VariableTableModel)getModel();
        model.setShowCode(!model.showCode);
        if (model.showCode) {
            TableColumn codeColumn = getColumnModel().getColumn((model.getCodeColumn()));
            if (codeColumn != null)
                codeColumn.setCellEditor(new DefaultCellEditor(codeComboBox));
        }
    }

    private void toggleShowDisp() {
        VariableTableModel model = (VariableTableModel)getModel();
        model.setShowDisp(!model.showDispPos);
        if (model.showDispPos) {
            TableColumn codeColumn = getColumnModel().getColumn((model.getDispColumn()));
            if (codeColumn != null)
                codeColumn.setCellEditor(new DefaultCellEditor(dispComboBox));
        }
    }

    private void toggleShowUnits() {
        VariableTableModel model = (VariableTableModel)getModel();
        model.setShowUnits(!model.showUnits);
        if (model.showUnits) {
            TableColumn codeColumn = getColumnModel().getColumn((model.getUnitsColumn()));
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
            for (Iterator it = eobj.getVariables(); it.hasNext(); ) {
                // only add attributes
                Variable var = (Variable)it.next();
                if (var.getKey().getName().startsWith("ATTR_"))
                    addVariable(var, eobj);
            }
        }
        owner = eobj;
    }

    /**
     * Add a Variable to be displayed
     */
    private void addVariable(Variable var, ElectricObject owner) {
        ((VariableTableModel)getModel()).addVariable(var, owner);
    }

    /**
     * Clear all variables from the table
     */
    private void clearVariables() {
        ((VariableTableModel)getModel()).clearVariables();
    }

    public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
        // reload vars
        ElectricObject eobj = owner;
        setElectricObject(null);
        setElectricObject(eobj);
    }

    public void databaseChanged(Undo.Change evt) {}

    public boolean isGUIListener() { return true; }

    // -------------------------------- JOBS ------------------------------

    /** Job to change a variable's value and code type */
    private static class VarChange extends Job {
        private Variable var;
        private ElectricObject owner;
        private Variable.Code code;
        private TextDescriptor.DispPos dispPos;
        private TextDescriptor.Unit unit;
        private Object newValue;
        private boolean display;

        private VarChange(Variable var, ElectricObject owner, Variable.Code code,
                          TextDescriptor.DispPos dispPos, TextDescriptor.Unit unit, Object newValue, boolean display) {
            super("Modify Attribute", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.var = var;
            this.owner = owner;
            this.code = code;
            this.dispPos = dispPos;
            this.unit = unit;
            this.newValue = newValue;
            this.display = display;
            startJob();
        }

        public boolean doIt() {
            Variable v = owner.updateVar(var.getKey(), newValue);
            v.setCode(code);
            TextDescriptor td = v.getTextDescriptor();
            if (td != null) {
                td.setDispPart(dispPos);
                td.setUnit(unit);
            }
            v.setDisplay(display);
            return true;
        }
    }

    /** Job to create a new attribute */
    private static class CreateAttribute extends Job {

        private String newName;
        private Object newValue;
        private ElectricObject owner;
        private Variable.Code code;
        private TextDescriptor.DispPos dispPos;
        private TextDescriptor.Unit units;

        private CreateAttribute(String newName, Object newValue, ElectricObject owner, Variable.Code code,
                                TextDescriptor.DispPos dispPos, TextDescriptor.Unit units) {
            super("Create Attribute", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.newName = newName;
            this.newValue = newValue;
            this.owner = owner;
            this.code = code;
            this.dispPos = dispPos;
            this.units = units;
            startJob();
        }

        public boolean doIt() {
            // get a unique name for the new Var if it already exists
            Variable var = owner.getVar(newName);
            int i = 0;
            boolean rename = false;
            while (var != null) {
                i++;
                var = owner.getVar(newName + "_" + i);
                rename = true;
            }
            if (rename) {
                String oldName = newName;
                newName = newName + "_" + i;
                System.out.println("Already an Attribute named "+oldName+", setting name to "+newName);
            }

            // create the attribute
            var = owner.newVar(newName, newValue);
            // if created on a cell, set the parameter and inherits properties
            if (owner instanceof Cell) {
                if (var == null) return false;
                TextDescriptor td = var.getTextDescriptor();
                td.setParam(true);
                td.setInherit(true);
            }

            // set other settings
            var.setCode(code);
            TextDescriptor td = var.getTextDescriptor();
            td.setDispPart(dispPos);
            td.setUnit(units);
            return true;
        }
    }

    /**
     * Class to delete an attribute in a new thread.
     */
    private static class DeleteAttribute extends Job
	{
        Variable var;
        ElectricObject owner;

        private DeleteAttribute(Variable var, ElectricObject owner)
        {
            super("Delete Attribute", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.var = var;
            this.owner = owner;
            startJob();
        }

        public boolean doIt()
        {
            if (var == null) return false;
            owner.delVar(var.getKey());
            return true;
        }
    }

}
