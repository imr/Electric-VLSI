package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
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
import java.awt.geom.Point2D;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;

/**
 * Class to define the Attributes panel in other dialogs.
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
            // adjust column index so that it is the column index as if all were shown
            int index = columnIndex;
            if (!showCode) index++;
            if (!showDispPos) index++;
            if (!showUnits) index++;

            if (index == 2) {
                return var.getCode();
            }
            if (index == 3) {
                TextDescriptor td = var.getTextDescriptor();
                if (td == null) return null;
                return td.getDispPart();
            }
            if (index == 4) {
                TextDescriptor td = var.getTextDescriptor();
                if (td == null) return null;
                return td.getUnit();
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
            TextDescriptor td = var.getTextDescriptor();

            if (col == 0) return;                   // can't change var name

            if (col == 1) {
                if (!aValue.toString().equals(var.getObject().toString())) {
                    VarChange job = new VarChange(var, owner, var.getCode(), td.getDispPart(), td.getUnit(),
                            aValue, var.isDisplay());
                    fireTableCellUpdated(row, col);
                }
            }

            if (col <= 1) return;

            // adjust column index so that it is the column index as if all were shown
            if (!showCode) col++;
            if (!showDispPos) col++;
            if (!showUnits) col++;

            if (col == 2) {
                Variable.Code newCode = (Variable.Code)aValue;
                if (newCode != var.getCode()) {
                    VarChange job = new VarChange(var, owner, newCode, td.getDispPart(), td.getUnit(),
                            var.getObject(), var.isDisplay());
                    fireTableCellUpdated(row, col);
                }
            }
            if (col == 3) {
                if (aValue == displaynone) {
                    if (var.isDisplay()) {
                        VarChange job = new VarChange(var, owner, var.getCode(), td.getDispPart(), td.getUnit(),
                                var.getObject(), false);
                        fireTableCellUpdated(row, col);
                    }
                } else {
                    TextDescriptor.DispPos newDispPos = (TextDescriptor.DispPos)aValue;
                    if (newDispPos != td.getDispPart()) {
                        VarChange job = new VarChange(var, owner, var.getCode(), newDispPos, td.getUnit(),
                                var.getObject(), true);
                        fireTableCellUpdated(row, col);
                    }
                }
            }
            if (col == 4) {
                TextDescriptor.Unit newUnit = (TextDescriptor.Unit)aValue;
                if (newUnit != td.getUnit()) {
                    VarChange job = new VarChange(var, owner, var.getCode(), td.getDispPart(), newUnit,
                            var.getObject(), var.isDisplay());
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

    private static JComboBox codeComboBox = null;
    private static JComboBox dispComboBox = null;
    private static JComboBox unitComboBox = null;
    private static final String displaynone = "None";

    /**
     * Create a new Attributes Table
     */
    public AttributesTable(boolean showCode, boolean showDispPos, boolean showUnits) {
        setGridColor(getBackground());          // hides cell grids
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setModel(new VariableTableModel(showCode, showDispPos, showUnits));

        initComboBoxes();

        // set up combo box editors
        int columnIndex = 2;
        if (showCode) {
            TableColumn codeColumn = getColumnModel().getColumn((columnIndex));
            if (codeColumn != null)
                codeColumn.setCellEditor(new DefaultCellEditor(codeComboBox));
            columnIndex++;
        }
        if (showDispPos) {
            TableColumn codeColumn = getColumnModel().getColumn((columnIndex));
            if (codeColumn != null)
                codeColumn.setCellEditor(new DefaultCellEditor(dispComboBox));
            columnIndex++;
        }
        if (showUnits){
            TableColumn codeColumn = getColumnModel().getColumn((columnIndex));
            if (codeColumn != null)
                codeColumn.setCellEditor(new DefaultCellEditor(unitComboBox));
            columnIndex++;
        }

        MouseListener mouseListener = new MouseAdapter() {
            public void MousePressed(MouseEvent e) {
                if (e.isShiftDown() || e.isControlDown() || e.isAltDown()) return;

                if (e.isPopupTrigger()) {
                    int row = rowAtPoint(e.getPoint());
                    int col = columnAtPoint(e.getPoint());
                    showPopupMenu(row, col, e.getPoint());
                }
            }
        };
        //addMouseListener(mouseListener);
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

    private void showPopupMenu(int row, int col, Point point) {
        System.out.println("Should show popup menu for row "+row+" col "+col);
    }

    // -----------------------------------------------------------------

    /** Job to change a variable's value and code type */
    public static class VarChange extends Job {
        private Variable var;
        private ElectricObject owner;
        private Variable.Code code;
        private TextDescriptor.DispPos dispPos;
        private TextDescriptor.Unit unit;
        private Object newValue;
        private boolean display;

        private VarChange(Variable var, ElectricObject owner, Variable.Code code,
                          TextDescriptor.DispPos dispPos, TextDescriptor.Unit unit, Object newValue, boolean display) {
            super("VarChange", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
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

    // -----------------------------------------------------------------

    private static class PopupListener extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
        }
    }

}
