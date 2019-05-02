package com.example.chemicalbalancer;

import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.logging.Logger;

/*
ASSUMPTIONS MADE
Input must be formatted like this C2H6 + O2 --> CO2 + H2O or like this

There may only be one polyatomic ion per molecule
There are no charges to balance
Polyatomic ions must be attached to an element such that the charges balance
Polyatomic ions must be attached to open and close brackets ()
 */

public class MainActivity extends AppCompatActivity {

    private TextInputLayout textInputChemRxn;

    private String userInput = null;
    private Button submitBtn;
    private TextView input;
    private TextView result;
    private List<String> elementStr;

    private static final Logger LOG = Logger.getLogger(MainActivity.class.getName());

    public static class StringAndInteger {
        public String element;
        public Integer elementNum;

        public StringAndInteger(String elements,
                                Integer elementNum) {
            this.element = elements;
            this.elementNum = elementNum;
        }
    }

    public static class IntLists2 {
        public List<Integer> capitalsIndex;
        public List<Integer> coefficientIndex;

        public IntLists2(List<Integer> capitalsIndex,
                         List<Integer> coefficientIndex) {
            this.capitalsIndex = capitalsIndex;
            this.coefficientIndex = coefficientIndex;
        }
    }

    //    Import this list from python script
    public static List<String> polyatomicList = Arrays.asList("(NH4)", "(NO2)", "(NO3)", "(NO4)", "(SO3)","(SO4)", "(SO2)", "(OH)");

    public  String balancedEquation (String input) throws ConcurrentModificationException {
//        receiving and splitting data into elements
        input = input.replaceAll("\\n", "");
        String[] eqnSplitter = input.split(" --> ");
        String[] rxnStrings = eqnSplitter[0].split(" \\+ ");
        String[] prodStrings = eqnSplitter[1].split(" \\+ ");
        List<String> elementStrings = elementStrings(rxnStrings, prodStrings);
        List<String> uniqueElements = uniqueElement(elementStrings, rxnStrings);
        List<List<StringAndInteger>> rankListEachMole = rankListEachMole(elementStrings);
        List<List<StringAndInteger>> rankList = new ArrayList<>();
        for (List<StringAndInteger> siList : rankListEachMole){
            rankList.add(zeroPutter(siList, uniqueElements));
        }
        double[][] rref = rref(matrix(rankList));

        List<Integer> balancedEquation = balancedNumbers(rref,uniqueElements,rxnStrings,prodStrings);
        String s = balancedEquation(elementStrings, balancedEquation);



//        This is for error (notifications) PUT AN IMAGE IN IF CONFIRMED USING this API
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();

        return s;
    }

    private void setViews () {
        submitBtn = findViewById(R.id.btn_submit);
        input = findViewById(R.id.txt_input);
        result = findViewById(R.id.txt_solved);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        textInputChemRxn = findViewById(R.id.txt_input);
//      CALL VALIDATE PROPER STRING FUNCTION

        setViews();
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rrefList.clear();
                result.setText(balancedEquation(input.getText().toString().trim()));
            }
        });

//        userInput = textInputChemRxn.getEditText().getText().toString().trim();
//        LOG.info("text input from client is registered as null");


    }

    private void setError (String error){
        textInputChemRxn.setError(error);
    }

    private boolean validateReaction (){
//        fill in method to check if chemical reaction will pass

        if(0 ==0 ){
            textInputChemRxn.setError(null);
//            textInputChemRxn.setErrorEnabled(false);
            return true;
        }
        textInputChemRxn.setError("Enter this equation.... ");
        return false;

    }
    public static Integer whereIsPolyatomic (String string, List<String> polyatomicElements ) {
        for(String str: polyatomicElements){
            if (string.contains(str)){
                return string.indexOf(str);
            }
        }
        return -1;
    }

    public static IntLists2 indexProducer(String oneElementString) {
        String string = oneElementString;
        List<Integer> capitalsIndex = new ArrayList<>();
        List<Integer> coefficientIndex = new ArrayList<>();
        for (int i = 0; i < oneElementString.length(); i++) {
            if (Character.isUpperCase(string.charAt(i)))
                capitalsIndex.add(i);
            if (Character.isDigit(string.charAt(i)))
                coefficientIndex.add(i);
        }
        IntLists2 holder = new IntLists2(capitalsIndex, coefficientIndex);
        return holder;
    }

    /*
    Consideration: this method as the polyatomic ion to te beginning of the list instead of at
    the end. This should be correct when the sort occurs.
    */
//
//    Cu + Ag(NO3) --> Ag + Cu(NO3)2

//    THIS FUNCTION IS FUCKED

    public static List<String> elementwNumParser(String string, List<Integer> capitalsIndex) {
        int ind = whereIsPolyatomic(string, polyatomicList);
        List<String> elementwNumList = new ArrayList<>();

        if (ind != -1){
            elementwNumList.add(string.substring(ind));
            string = string.substring(0, ind);
            for (int i=capitalsIndex.size()-1; i>-1; i--){
                if (ind < capitalsIndex.get(i)){
                    capitalsIndex.remove(i);
                }
            }
        }
        if (capitalsIndex.size() ==1){
            if (capitalsIndex.get(0) == 0){
                elementwNumList.add(string);
            } else {
//                throw StringIndexOutOfBoundsException;
                LOG.info("Issue processing and parsing string");
            }
        } else {

            for (int i = 0; i < capitalsIndex.size(); i++) {
                if (i == capitalsIndex.size() - 1) {
                    elementwNumList.add(string.substring(capitalsIndex.get(i)));
                } else {
                    elementwNumList.add(string.substring(capitalsIndex.get(i), capitalsIndex.get(i + 1)));
                }
            }
        }
        return elementwNumList;

    }

    //
//    BROKENN!!!!
    public static List<String> uniqueElement(List<String> moleStrList, String[] rxnArr) {

        List<String> uniqueElements = new ArrayList<>();
        for (int j = 0; j < rxnArr.length; j++) {

            String string = moleStrList.get(j);
            List<String> elementwNumList = elementwNumParser(string, indexProducer(string).capitalsIndex);

            for (String elementWnum : elementwNumList){
                String element = elementInfoOneMole(elementWnum).element;
                if (!uniqueElements.contains(element)) {
                    uniqueElements.add(element);
                }
            }
        }

        Collections.sort(uniqueElements);
        return (uniqueElements);
    }

    //
    public static StringAndInteger elementInfoOneMole(String elementwNum) {
//        This passes a single element wihtin a molecule string, for example it would pass O2 or Ca for example
        String string = elementwNum;
        if (!string.contains("(")) {
            List<Integer> digitIndex = new ArrayList<>();
            for (int i = 0; i < string.length(); i++) {
                if (Character.isDigit(string.charAt(i))) {
                    digitIndex.add(i);
                    break;
                }
            }
            if (digitIndex.size() != 0) {
                String element = string.substring(0, digitIndex.get(0));
                int elementNum = Integer.valueOf(string.substring(digitIndex.get(0)));
                StringAndInteger enn = new StringAndInteger(element, elementNum);
                return enn;
            } else {
                String element = string;
                int elementNum = 1;
                StringAndInteger enn = new StringAndInteger(element, elementNum);
                return enn;
            }
        } else {
            int elementNum = 1;
            if (!string.substring(string.lastIndexOf(")")).equals(")")) {
                elementNum = Integer.parseInt(string.substring(string.lastIndexOf(')') + 1));
            }
            String polyatomic = string.substring(string.indexOf('(')+1, string.lastIndexOf(')'));
            StringAndInteger enn = new StringAndInteger(polyatomic, elementNum);
            return enn;
        }
    }
    //    Pb(SO4) > 10PBSO3 + 3O2
//
//    Note: Polyatomic ions are always listed first in the rankLIst
    public static List<List<StringAndInteger>> rankListEachMole(List<String> elementStrings) {
        List<List<StringAndInteger>> rankListEachMole = new ArrayList<>();
//        String == "C2H6"
        for (String string : elementStrings) {
            List<Integer> capitalIndex = indexProducer(string).capitalsIndex;
//            singleRxnElewNums.get(O) == C2
            List<String> singleRxnElewNums = elementwNumParser(string, capitalIndex);
            Collections.sort(singleRxnElewNums);
            List <StringAndInteger> elementInfoList = new ArrayList<>();
            for (String s : singleRxnElewNums){
                StringAndInteger elementInfo = elementInfoOneMole(s);
                elementInfoList.add(elementInfo);
            }
            rankListEachMole.add(elementInfoList);
        }
        return rankListEachMole;
    }

    //    Needs to be interated through rankList
    public static List<StringAndInteger> zeroPutter (List<StringAndInteger> rankList, List<String> uniqueElements){
        List<StringAndInteger> zeroPutter = new ArrayList<>();
        Map<Integer, StringAndInteger> hashMap = new HashMap<>();
        for (int i=0; i<uniqueElements.size(); i++){
            List<StringAndInteger> elementInfoList = new ArrayList<>();
            for (StringAndInteger si : rankList){
                Boolean s = isUniqueElement(rankList, si);
                if(si.element.equals(uniqueElements.get(i)) && isUniqueElement(elementInfoList, si)){
                    elementInfoList.add(si);
                    hashMap.put(i, si);
                }
            }
        }
        for (int i=0; i<uniqueElements.size(); i++){
            if (hashMap.containsKey(i)){
                zeroPutter.add(i, hashMap.get(i));
            }else{
                StringAndInteger empty = new StringAndInteger(uniqueElements.get(i), 0);
                zeroPutter.add(empty);
            }
        }
        return zeroPutter;
    }

    public static double[][] matrix(List<List<StringAndInteger>> rankList) {
//         3
        int m = rankList.get(0).size();
//         4
        int n = rankList.size();
        double[][] matrix = new double[m][n];
        List<List<StringAndInteger>> columnList = rankList;
        //        For each column
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < m; i++) {
                matrix[i][j] = rankList.get(j).get(i).elementNum;
            }
        }

        return matrix;
    }
    private static List<double[][]> rrefList = new ArrayList<>();

    private static boolean isUnique (double[][] rref){
        Set<double[][]> rrefSet = new HashSet<>(rrefList);
        return rrefSet.size() == rrefList.size();
    }
    //        C2H6 + O2 --> CO2 + H2O
    private static double[][] rref(double[][] matrix) {
        double[][] rref = matrix;
        Set<double[][]> rrefSet = new HashSet<>(rrefList);
        while (isUnique(rref)){
            rref = rowReducer(rref);
        }

        rref = orderMatrixRows(rref);
        rref = reducedMatrix(rref);
        return rref;
    }

    private static Map<int[], Double> pivotMapper(double[][] matrix) {
        double[][] rref = matrix;
        List<Integer> pivotRows = new ArrayList<>();

//        key for hashmap will be the column # and the value will be the pivot value
        Map<int[], Double> pivotMap = new HashMap<>();
        for (int t = 0; t < rref[0].length; t++) {
            double pivot = -1;
            int pivotRow = 0;
            for (int s = 0; s < rref.length; s++) {
                double st = rref[s][t];
//                there can only be one pivot per column
                if (st != 0 && pivot == -1 && !pivotRows.contains(s)) {
                    pivot = rref[s][t];
                    pivotRows.add(s);
                    int[] array = new int[]{s, t};
                    pivotMap.put(array, pivot);
                }
            }
        }
        pivotMap = mapSorter(pivotMap);
        return pivotMap;

    }

    private static double[][] rowReducer(double[][] matrix) {
        double[][] rref = matrix;
        Map<int[], Double> pivotMap = (pivotMapper(rref));

        for (Map.Entry<int[], Double> entry : pivotMap.entrySet()) {
            boolean isChanged = false;
            double pivot = entry.getValue();
            int i = entry.getKey()[0];
            int j = entry.getKey()[1];
            double[] pivotRow = rref[i];

            for (int s = 0; s < rref.length; s++) {
                double mul = -(rref[s][j]/ pivot);
                if (rref[s][j] != 0 && s != i){
                    for (int t=0; t<rref[0].length; t++){
                        double d = rref[s][j];
                        rref[s][t] = rref[s][t] + mul * rref[i][t];
                        isChanged =true;
                    }
                }
            }
            if (isChanged)
                break;

        }
        rrefList.add(rref);
        return rref;
    }

    private static Map<int[], Double> mapSorter(Map<int[], Double> pivotMap) {
        Map<int[], Double> hashMap = new LinkedHashMap<>();
        List<Map.Entry<int[], Double>> entryList = new ArrayList<>(pivotMap.entrySet());

        Collections.sort(entryList, new Comparator<Map.Entry<int[], Double>>() {
            @Override
            public int compare(Map.Entry<int[], Double> o1, Map.Entry<int[], Double> o2) {
                return o1.getKey()[1] - o2.getKey()[1];
            }
        });

        for (Map.Entry<int[], Double> e : entryList){
            hashMap.put(e.getKey(), e.getValue());
        }
        return hashMap;
    }

    private static double[][] orderMatrixRows(double[][] matrix) {
        List<double[]> rows = new ArrayList<>();
        List<double[]> nonPivotRows = new ArrayList<>();
        double[][] rref = new double[matrix.length][matrix[0].length];
        for (int t = 0; t < matrix[0].length - 1; t++) {
            for (int s = 0; s < matrix.length; s++) {
                if (matrix[s][t] != 0) {
                    double[] row = matrix[s];
                    rows.add(row);
                }
            }
        }
        for (int i = 0; i < rows.size(); i++) {
            rref[i] = rows.get(i);
        }
        return rref;
    }

    private static double[][] reducedMatrix(double[][] matrix) {
        List<double[]> doubList = new ArrayList<>();
        double[][] rref = matrix;
        List<Double> pivotList = new ArrayList<>();
        for (double[] doub : matrix) {
            for (double d : doub) {
                if (d != 0) {
                    pivotList.add(d);
                    break;

                }
            }
        }
        for (int i = 0; i < matrix.length; i++) {
            double[] doub = matrix[i];
            double[] row = new double[doub.length];
            double pivot = pivotList.get(i);
            for (int j = 0; j < doub.length; j++) {
                if (doub[j] != 0) {
                    double val = doub[j] / pivot;
                    row[j] = val;
                }
            }
            doubList.add(row);
        }

        for (int i = 0; i < doubList.size(); i++) {
            rref[i] = doubList.get(i);
        }
        return rref;
    }

    public static List<Integer> balancedNumbers (double[][] rref, List<String> uniqueElements, String[] rxnStrings, String[] prodStrings) {
        DecimalFormat df = new DecimalFormat("#.#####");
        df.setRoundingMode(RoundingMode.HALF_UP);
        List<Double> rawCoefficientList = new ArrayList<>();
//        rref.length gives the number of ROWS
        for (int i=0; i<rref.length; i++){
//            rref[j].length gives the number of COLUMNS
            rawCoefficientList.add(rref[i][rref[i].length-1]);
        }
        int lowestPossibleCount =0;
        for (int count=1; count<33; count++) {
            List<Boolean> isIntegerList = new ArrayList<>();
            List<Double> doubleList = new ArrayList<>();
            for (Double d : rawCoefficientList) {
                double doub = Double.parseDouble(df.format(d * count));
                if (doub % 1 == 0) {
                    isIntegerList.add(true);
                }
            }
            if (isIntegerList.size()==rawCoefficientList.size()){
                lowestPossibleCount = count;
                break;
            }
        }
        for (int i=rawCoefficientList.size()-1; i>-1; i--){
            if (rawCoefficientList.get(i) == 0){
                rawCoefficientList.remove(i);
            }

        }
//        if count != 0 || null;
        List<Integer> balancedNums = new ArrayList<>();
        for (Double d : rawCoefficientList){
            int balancedInt = Math.abs((int) Math.round(d*lowestPossibleCount));
            balancedNums.add(balancedInt);
        }
        balancedNums.add(lowestPossibleCount);

        return balancedNums;
    }

    //        Cu + Ag(NO3) --> Ag + Cu(NO3)2
    public static String balancedEquation(List<String> elementStrings, List<Integer> balancedNumbers){
        int reactantsIndex = 0;
        int count =-1;
        for (int i : balancedNumbers) {
            count++;
            if (i<0) {
                break;
            }
        }
        List<String> rxnElements = new ArrayList<>();
        List<String> prodElements = new ArrayList<>();
        for (int i=0; i<elementStrings.size(); i++ ){
            if ( i< count-1){
                rxnElements.add(elementStrings.get(i));
            } else{
                prodElements.add(elementStrings.get(i));
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        int commonInt =0;
        for (int i=0; i<rxnElements.size(); i++) {
            stringBuilder.append(balancedNumbers.get(i) + " " + rxnElements.get(i) + " + ");
            commonInt=i;
        }
        stringBuilder.delete(stringBuilder.lastIndexOf("+ "), stringBuilder.length());
        stringBuilder.append(" ---> ");
        for (int i=0; i<prodElements.size(); i++) {
            stringBuilder.append(balancedNumbers.get(i+commonInt+1) + " " + prodElements.get(i) + " + ");
        }
        stringBuilder.delete(stringBuilder.lastIndexOf("+ "), stringBuilder.length());
        String balancedEqn = stringBuilder.toString();
        return balancedEqn;
    }

//        C3H8 + O2 --> CO2 + H2O
//        C2H6 + O2 --> CO2 + H2O
//        CH6 + O2 --> CO2 + H2O
//        Cu + Ag(NO3) --> Ag + Cu(NO3)2
//        Al2O3 + Fe --> Fe3O4 + Al
//        P4O10 + H2O --> H3PO4
//        Al + H2(SO4) --> Al2(SO4)3 + H2

//WHY CANT IT SOLVE THIS ONE???
//        Al2(SO3)3 + NaOH --> Na2(SO3) + AlO3H3


    /*    ==================================================
      HELPER FUNCTIONS
*/
    public static List<String> elementStrings(String[] rxnStrings, String[] prodStrings) {

        List<String> strings = new ArrayList<>();
        for (int i = 0; i < rxnStrings.length; i++) {
            if (rxnStrings[i] != null)
                strings.add(rxnStrings[i]);
        }
        for (int i = 0; i < prodStrings.length; i++) {
            if (prodStrings[i] != null){
                strings.add(prodStrings[i]);
            }


        }
        return strings;
    }

    public static Boolean isUniqueElement (List<StringAndInteger> rankList, StringAndInteger elementInfo) {
        List<Integer> intList = new ArrayList<>();
        for (StringAndInteger s : rankList){
            if(elementInfo.element.equals(s.element)){
                intList.add(0);
            }
        }
        if (intList.size() == 0){
            return true;
        }
        return false;
    }
}
