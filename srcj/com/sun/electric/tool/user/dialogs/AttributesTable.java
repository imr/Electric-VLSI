package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Jun 24, 2004
 * Time: 11:22:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class AttributesTable extends JTable {

    /**
     * Model for storing Table data
     * */
    private static class VariableTableModel extends AbstractTableModel {

        private List varInfos;                     // list of variables to display
        private boolean DEBUG = false;                      // if true, displays database var names
        private static final String [] columnNames = { "Name", "Value", "Code" };
        private boolean showCode = true;

        // Class to hold var and owner pair
        private static class VarInfo {
            private Variable var;
            private ElectricObject owner;
        }

        // constructor
        private VariableTableModel(boolean showCode) {
            varInfos = new ArrayList();
            this.showCode = showCode;

        }

        /** Get number of columns in table */
        public int getColumnCount() {
            int c = 2;
            if (showCode) c++;
            return c;
        }

        /** Get number of rows in table */
        public int getRowCount() {
            return varInfos.size();
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
            } else if (columnIndex == 1) {
                // value
                return var.getObject().toString();
            } else if (columnIndex == 2 && showCode) {
                return var.getCode();
            }

            return null;
        }

        /** Get column header names */
        public String getColumnName(int col) {
            return columnNames[col];
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

            if (col == 1) {
                if (!aValue.toString().equals(var.getObject().toString())) {
                    VarChange job = new VarChange(var, owner, var.getCode(), aValue);
                    fireTableCellUpdated(row, col);
                }
            }
            if (col == 2) {
                Variable.Code newCode = (Variable.Code)aValue;
                if (newCode != var.getCode()) {
                    VarChange job = new VarChange(var, owner, newCode, var.getObject());
                    fireTableCellUpdated(row, col);
                }

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
            if (col == 2) return Variable.Code.class;
            return String.class;
        }
    }

    // ----------------------------------------------------------------

    private JComboBox codeComboBox;

    /**
     * Create a new Attributes Table
     */
    public AttributesTable(boolean showCode) {
        setGridColor(getBackground());          // hides cell grids
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setModel(new VariableTableModel(showCode));

        // set up combo box to specify code
        if (showCode) {
            codeComboBox = new JComboBox();
            for (Iterator it = Variable.Code.getCodes(); it.hasNext(); ) {
                codeComboBox.addItem(it.next());
            }
            codeComboBox.setFont(new Font("Dialog", 0, 12));
        }
        TableColumn codeColumn = getColumnModel().getColumn((2));
        if (codeColumn != null)
            codeColumn.setCellEditor(new DefaultCellEditor(codeComboBox));
    }

    /**
     * Add a Variable to be displayed
     */
    public void addVariable(Variable var, ElectricObject owner) {
        ((VariableTableModel)getModel()).addVariable(var, owner);
    }

    /**
     * Clear all variables from the table
     */
    public void clearVariables() {
        ((VariableTableModel)getModel()).clearVariables();
    }

    // -----------------------------------------------------------------

    /** Job to change a variable's value and code type */
    public static class VarChange extends Job {
        private Variable var;
        private ElectricObject owner;
        private Variable.Code code;
        private Object newValue;

        private VarChange(Variable var, ElectricObject owner, Variable.Code code, Object newValue) {
            super("VarChange", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.var = var;
            this.owner = owner;
            this.code = code;
            this.newValue = newValue;
            startJob();
        }

        public boolean doIt() {
            Variable v = owner.updateVar(var.getKey(), newValue);
            v.setCode(code);
            return true;
        }
    }


}
