import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        FileReader rdr;
        BufferedReader reader;
        String[][] sortArray;
        String strConsole = "";
        String line = "";
        ArrayList<String> file = new ArrayList<>(); //Здесь храним не весь файл, а только имена аэропортов
        ArrayList<Long> line_skipSize = new ArrayList<>();

        try {
            rdr = new FileReader("airports.csv");
            reader = new BufferedReader(rdr);

            line = reader.readLine();

            //Выписываем значения аэропортов
            while (line != null) {
                file.add(line.split(",")[1]);
                line_skipSize.add((long) line.length() + 1);
                line = reader.readLine();
            }

            reader.close();
            rdr.close();

            //Делаем сортированный массив для того, чтобы проще было найти значение по двоичному поиску элементов
            sortArray = new String[file.size()][2];

            for (int i = 0; i < file.size(); i++) {
                sortArray[i][0] = i + "";
                sortArray[i][1] = file.get(i).replace("\"", "");
            }

            Arrays.sort(sortArray, Comparator.comparing(arr -> arr[1]));

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        do {
            try {
                //Далее запрашиваем фильтры и начальное значение
                Scanner input = new Scanner(System.in);

                System.out.println("Введите фильтр:");
                strConsole = input.nextLine();

                if (strConsole.equals("!quit"))
                    break;

                var filter = Convert_Filters(strConsole);

                System.out.println("Введите первые буквы аэропорта:");
                strConsole = input.nextLine();

                if (strConsole.equals("!quit"))
                    break;

                var name = strConsole;
                rdr = new FileReader("airports.csv");
                reader = new BufferedReader(rdr);

                //Находим нужные имена и номера строк
                long startTime = System.nanoTime(); //Начало таймера
                var find_index_airport = find_airport_name(name, sortArray);



                //Далее повторям открытие файла и считывание уже готовых строк, которые необходимы для вывода.
                if (find_index_airport.size() == 0) {
                    System.out.println("Значений не найдено");
                    continue;
                }

                ArrayList<String> resultLines = new ArrayList<>();

                int startIndex = 0;


                for (int j = 0; j < find_index_airport.size(); j++) {
                    for (int i = startIndex; i < find_index_airport.get(j); i++) {
                        reader.skip(line_skipSize.get(i));
                    }

                    resultLines.add(reader.readLine());
                    startIndex = find_index_airport.get(j) + 1;
                }

                find_airports_filter(filter, resultLines);
                long endTime = System.nanoTime(); //Конец таймера

                resultLines.sort(Comparator.comparing(value -> value.split(",")[1]));

                for (String str : resultLines) {
                    System.out.println(str.split(",")[1] + "[" + str + "]");
                }
                System.out.println("Кол-во найденных строк: " + resultLines.size() +
                        " Время, затраченное на поиск: "+ (endTime - startTime)/1000000 + "мс\n\n\n");


            } catch (MissingFormatArgumentException | IOException | NumberFormatException e) {
                throw new RuntimeException(e);
            } finally {
                reader.close();
                rdr.close();
            }
        } while (strConsole != "!quit");
    }

    private static void find_airports_filter(String[][] filter, ArrayList<String> result) {
        if (filter.length == 0)
            return;
        for (int i = 0; i < result.size(); i++) {
            String group = filter[0][3];
            var line = result.get(i).split(",");
            boolean answer = stupid_converter_Filter(filter[0], line);

            for (int j = 1; j < filter.length; j++) {
                if (filter[j][3].equals(group)) {
                    answer = answer && stupid_converter_Filter(filter[j], line);
                } else {
                    answer = answer || stupid_converter_Filter(filter[j], line);

                    group = filter[j][3];
                }
            }

            if (!answer) {
                result.remove(i);
                i--;
            }
        }
    }

    private static boolean stupid_converter_Filter(String[] filter, String[] line) {
        int index = Integer.parseInt(filter[0]) - 1;
        if (filter[1].equals("<")) {
            int value = Integer.parseInt(filter[2]);

            if (index != 0) {
                return line[index].length() < value + 2;
            } else {
                return Integer.parseInt(line[index]) < value;
            }
        } else if (filter[1].equals(">")) {
            int value = Integer.parseInt(filter[2]);

            if (index != 0) {
                return line[index].length() > value + 2;
            } else {
                return Integer.parseInt(line[index]) > value;
            }
        } else {
            return line[index].toLowerCase().replace("\"", "").
                    equals(filter[2].toLowerCase().replace("'", ""));
        }
    }

    private static ArrayList<Integer> find_airport_name(String name, String[][] result) {
        int i = result.length;

        i = find_value_index(name, result);

        ArrayList<Integer> index = new ArrayList<>();

        if (result[i][1].toLowerCase().charAt(0) == name.toLowerCase().charAt(0)) {
            if (result[i][1].toLowerCase().startsWith(name.toLowerCase())) {
                index.add(Integer.parseInt(result[i][0]));
            }
        }

        if (i - 1 >= 0 && result[i - 1][1].toLowerCase().charAt(0) == name.toLowerCase().charAt(0)) {
            int j = i - 1;

            while (j >= 0 && result[j][1].toLowerCase().charAt(0) == name.toLowerCase().charAt(0)) {
                if (result[j][1].toLowerCase().startsWith(name.toLowerCase())) {
                    index.add(Integer.parseInt(result[j][0]));
                }
                j--;
            }
        }

        if (i + 1 < result.length && result[i + 1][1].toLowerCase().charAt(0) == name.toLowerCase().charAt(0)) {
            int j = i + 1;

            while (j < result.length && result[j][1].toLowerCase().charAt(0) == name.toLowerCase().charAt(0)) {
                if (result[j][1].toLowerCase().startsWith(name.toLowerCase())) {
                    index.add(Integer.parseInt(result[j][0]));
                }
                j++;
            }
        }

        index.sort(Comparator.naturalOrder());

        return index;
    }

    private static int find_value_index(String value, String[][] result) {
        int index = result.length / 2;

        for (int count = 1; count < Math.log(result.length); count++) {
            if (index < 0) {
                index = 0;
            } else if (index >= result.length) {
                index = result.length - 1;
            }

            if (result[index][1].toLowerCase().charAt(0) < value.toLowerCase().charAt(0)) {
                index += result.length / Math.pow(2, count);
            } else if (result[index][1].toLowerCase().charAt(0) > value.toLowerCase().charAt(0)) {
                index -= result.length / Math.pow(2, count);
            }
        }

        if (index < 0) {
            index = 0;
        } else if (index >= result.length) {
            index = result.length - 1;
        }

        return index;
    }

    //Представляем, что в фильтре недопустимо писать скобки
    private static String[][] Convert_Filters(String filter) {
        var filters = filter.split("column|.column");
        filters = Arrays.copyOfRange(filters, 1, filters.length);
        String[][] flt = new String[filters.length][4]; //Flt - массив для удобства применения фильтров

        int group = 0;

        //Начало конвертации filters в flt
        for (int i = 0; i < filters.length; i++) {
            if (!filters[i].startsWith("["))
                throw new MissingFormatArgumentException("Not illegal format in Filter");

            var tempSChar = filters[i].toCharArray();


            if (!tryParse("" + tempSChar[1])) {
                throw new NumberFormatException("Not illegal format in Filter");
            }

            flt[i][0] = tempSChar[1] + "";                                        //Записываем корректный номер столбца

            if (tempSChar[3] == '>' || tempSChar[3] == '<' || tempSChar[3] == '=') {
                flt[i][1] = tempSChar[3] + "";                                    //Записываем корретный знак сравнения
            } else {
                throw new MissingFormatArgumentException("Not illegal format in Filter");
            }

            if (tempSChar[3] != '=') {
                if (!tryParse(tempSChar[4] + "")) {
                    throw new MissingFormatArgumentException("Not illegal format in Filter");
                }
            }

            StringBuilder tempRes = new StringBuilder(); //Значение конвертируем в знаки
            int j;

            for (j = 4; j < tempSChar.length; j++) {
                if (tempSChar[j] != '\'' && tempSChar[j] != '\"') {
                    if (tempSChar[j] == '|' || tempSChar[j] == '&')
                        break;
                    tempRes.append(tempSChar[j]);
                }
            }

            flt[i][2] = tempRes.toString();                                                   //Записываем значение фильтра

            flt[i][3] = group + "";                                     //Записываем номер группы для разделения & и |

            if (j < tempSChar.length && tempSChar[j] == '|') {
                group++;
            }
        }

        return flt;
    }

    private static boolean tryParse(String line) {
        try {
            Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}