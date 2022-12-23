//sezin laleli 191101040

import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.security.Key;
import java.util.*;

public class Table {
    List<Row> rows = null;
    String[] columns = null;

    /***
     * You may more constructors
     * @param path
     */
    Table(String path) {
        reloadFromFile(path);
    }

    // to return new table objects when needed
    Table(String[] hdr, List<Row> rws) {
        columns = hdr;
        rows = rws;
        if (rows != null) {
            buildIndexes();
        }
    }

    void populateHeader(String header) {
        columns = header.split(",");
    }

    void populateData(Scanner myReader) {
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            rows.add(new Row(data));
        }
    }

    Row getRow(int index) {
        return rows.get(index);
    }

    public void reloadFromFile(String path) {
        rows = new LinkedList<Row>(); //resets

        try {
            File myObj = new File(path);
            Scanner myReader = new Scanner(myObj);
            if (myReader.hasNextLine()) { //This is supposed to be the header
                populateHeader(myReader.nextLine());
                populateData(myReader);
            }
            myReader.close();
            buildIndexes();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    /***
     *  This will iterate over the columns and build BTree based indexes for all columns
     */

    BTree<String, List<Integer>>[] BArray = null;

    private void buildIndexes() {
        giveColumnIndexes();
        // create one for every column and put them in an arraylist
        BArray = new BTree[columns.length];

        for (int i = 0; i < columns.length; i++) {
            BTree<String, List<Integer>> st = new BTree<String, List<Integer>>();
            for (int j = 0; j < rows.size(); j++) {
                List<Integer> indexes = new ArrayList<>();
                //if that key didn't already exist
                if (st.get(getRow(j).getColumnAtIndex(i)) == null) {
                    indexes.add(j);
                    st.put(getRow(j).getColumnAtIndex(i), indexes);
                }
                //if that key already existed
                else {
                    int oldLen = st.get(getRow(j).getColumnAtIndex(i)).size();
                    st.get(getRow(j).getColumnAtIndex(i)).add(oldLen, j);
                }
            }
            BArray[i] = st;
        }
    }

    BTree<String, Integer> ColumnI = null;

    private void giveColumnIndexes() {
        ColumnI = new BTree<String, Integer>();
        for (int i = 0; i < columns.length; i++) {
            ColumnI.put(columns[i], i);
        }
    }

    private <T> List<T> intersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<T>();

        for (T t : list1) {
            if (list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }

    /***
     *This method is supposed to parse the filtering statement
     * identify which rows will be filtered
     * apply filters using btree indices
     * collect rows from each btree and find the mutual intersection of all.
     * Statement Rules: ColumnName==ColumnValue AND ColumnName==ColumnValue AND ColumnName==ColumnValue
     * Can be chained by any number of "AND"s; 0 or more
     * sample filterStatement: first_name==Roberta AND id=3
     * Return Type: A new Table which consists of Rows that pass the filter test
     */
    public Table filter(String filterStatement) {
        String[] temp = filterStatement.split(" AND ");
        int[] keyI = new int[temp.length];  // column indexes
        String[] value = new String[temp.length];  // column value
        for (int i = 0; i < temp.length; i++) {
            String[] temp2 = temp[i].split("==");
            keyI[i] = ColumnI.get(temp2[0]);
            value[i] = temp2[1];
        }
        List<List<Integer>> tmp = new LinkedList<>();
        List<Row> rw = new LinkedList<>();
        for (int i = 0; i < keyI.length; i++) {
            if (BArray[keyI[i]].get(value[i]) != null) {
                tmp.add(BArray[keyI[i]].get(value[i]));
            } else {
                System.out.println("no match");
                return new Table(columns, null);
            }
        }
        if (tmp.size() == 0) {
            System.out.println("no match");
            return new Table(columns, null);
        }
        if (keyI.length == 1) {
            for (int j = 0; j < tmp.get(0).size(); j++) {
                rw.add(getRow(tmp.get(0).get(j)));
            }
        }
        //if there are more than one pile return their intersection
        else {
            List<Integer> ind_temp = intersection(tmp.get(0), tmp.get(1));
            for (int i = 2; i < tmp.size(); i++) {
                ind_temp = intersection(ind_temp, tmp.get(i));
            }
            for (int i = 0; i < ind_temp.size(); i++) {
                rw.add(getRow(ind_temp.get(i)));
            }
        }

        Table tb = new Table(columns, rw);
        return tb;
    }

    /***
     * This method projects only set of columns from the table and forms a new table including all rows but only selected columns
     * columnsList is comma separated list of columns i.e., "id,email,ip_address"
     */
    public Table project(String columnsList) {
        String[] temp = columnsList.split(",");
        if (rows == null) {
            return new Table(temp, null);
        }
        List<Row> rw = new LinkedList<>();
        for (int i = 0; i < rows.size(); i++) {
            String temp2 = "";
            for (int j = 0; j < temp.length; j++) {
                temp2 += rows.get(i).getColumnAtIndex(ColumnI.get(temp[j])) + ",";
            }
            temp2 = temp2.substring(0, temp2.length() - 1);
            rw.add(new Row(temp2));
        }
        Table tb = new Table(temp, rw);
        return tb;
    }

    /***
     *  Print column names in the first line
     *  Print the rest of the table
     */

    public void show() {
        if (rows != null) {
            System.out.println(String.join(",", columns) + "\n");
            for (Row rw : rows) {
                System.out.println(rw.toString() + "\n");
            }
        }
    }

    public static void main(String[] args) {
        Table tb = new Table("userLogs.csv");//change path as you like

        //tb.show(); // should print everything
        tb.filter("id==3").project("first_name").show();  // should print Aldon


        //This is suppposed to print Jobling,sjoblingi@w3.org
        tb.filter("id==19 AND ip_address==242.40.106.103").project("last_name,email").show();

        //amathewesg@slideshare.net
        //imathewesdx@ucoz.com
        tb.filter("last_name==Mathewes").project("email").show();

        //We might test with a different csv file with same format but different column count
        //Good luck!!
    }
}