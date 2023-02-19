package ru.pugovishnikova;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.*;

public class practics{

    public static int COUNT = 2;//число операторов, в нашем случае двоё кассиров
    public static float LEFTTRANS = 0.0f;//мимнимальная граница интервала времени возможного поступления покупателя
    public static float RIGHTTRANS = 26.0f;//максимальная граница интервала времени возможного поступления покупателя
    public static float LEFTOP1 = 15.0f;//мимнимальная граница интервала времени обработки покупателя первым кассиром
    public static float RIGHTOP1 = 37.0f;//миаксимальаня граница интервала времени обработки покупателя первым кассиром
    public static float LEFTOP2 = 20.0f;//мимнимальная граница интервала времени обработки покупателя вторым кассиром
    public static float RIGHTOP2 = 32.0f;//максимальная граница интервала времени обработки покупателя вторым кассиром


    public static void main(String[] args) throws IOException {

        File logFile = new File("logFile1.txt");
        Files.deleteIfExists(logFile.toPath());
        PrintWriter pw = new PrintWriter(logFile);

        Operator[] operators = new Operator[COUNT];
        operators[0] = new Operator(LEFTOP1, RIGHTOP1, 1, pw);
        operators[1] = new Operator(LEFTOP2, RIGHTOP2, 2, pw);
        Model newModel = new Model(operators, pw);

        while (newModel.getCurrentTime()<3600.0f) {
            newModel.process();
        }

        newModel.operators[0].clear();
        newModel.operators[1].clear();
        newModel.operators[0]=null;
        newModel.operators[1]=null;
        pw.close();

    }


    static class Queue {
        private LinkedList<Transaction> queue;

        public Queue() {
            this.queue = new LinkedList<>();
        }

        public void addToQ(Transaction transaction) {
            queue.addLast(transaction);
        }

        public void removeTrans() {
            queue.removeFirst();
        }

        public Transaction getTrans() {
            return queue.getFirst();
        }

        public int size() {
            return queue.size();
        }
    }

    static class Transaction {
        private float time;
        private final int ID;
        public Transaction(float globalTime, int number) {
            this.time = globalTime;
            this.ID = number;
        }

        public int getID() {
            return ID;
        }

        public float getTime() {
            return time;
        }
    }

    static class Operator {
        private final PrintWriter pw;
        final private int ID;
        final private float leftBoard;//левая граница интервала обслуживания
        final private float rightBoard;//правая граница интервала обслуживания
        private float timeOut;//время выхода

        public void setTimeOut(float timeOut) {
            this.timeOut = timeOut;
            this.currentTransaction = null;
        }

        private Queue QUEUE;
        private Transaction currentTransaction;

        public Operator(float leftBoard, float rightBoard, int ID, PrintWriter pw) {
            this.pw = pw;
            this.ID = ID;
            this.leftBoard = leftBoard;
            this.QUEUE = new Queue();
            this.rightBoard = rightBoard;
            this.timeOut = 10000000000f;
        }

        public Queue getQUEUE() {
            return QUEUE;
        }

        public boolean checkEmpty() {
            return currentTransaction == null;
        }

        public Transaction getCurrentTransaction() {
            return currentTransaction;
        }

        public void setCurrentTransaction(Transaction currentTransaction) {
            this.currentTransaction = currentTransaction;
        }

        public void operatorProcess(Transaction transaction, int pos) {

            if (currentTransaction == null) {
                currentTransaction = transaction;
                switch (pos) {
                    case 1 -> {//обработка только что поступившего транзакта
                        pw.printf("In %fs transaction ID#%d occupied the device number %d\n", currentTransaction.getTime(), currentTransaction.getID(), ID);
                        timeOut = currentTransaction.getTime() + generate(leftBoard, rightBoard);
                    }
                    case 2 -> {//обработка транзакта из очереди
                        pw.printf("In %fs transaction ID#%d occupied the device number %d\n", timeOut, currentTransaction.getID(), ID);
                        timeOut += generate(leftBoard, rightBoard);

                    }
                }
            } else {
                QUEUE.addToQ(transaction);
                pw.printf("In %fs transaction ID#%d entered the queue number %d\n", transaction.getTime(), transaction.getID(), ID);
            }
        }


        public void clear(){
            QUEUE = null;
        }
    }

    static class Model {
        private float newTransactionTime;
        private boolean flag;//неободимо ли в данный момент в модели сгенирировать новый возможный приход транзакта или еще сущетсвует не поступивший в очередь или кассиров
        final private PrintWriter pw;
        private int transactionCount;//номер текущего поступившего транзакта
        final private Operator[] operators;
        private Transaction transaction;
        private float currentTime;//глобальное текущее время

        public Model(Operator[] operators, PrintWriter pw) {
            flag = true;
            this.pw = pw;
            this.operators = operators;
            this.newTransactionTime=0.0f;
            this.transactionCount = 0;
        }


        public float getCurrentTime() {
            return currentTime;
        }


        //выбор оператора, к которому необходимо направить текущий транзакт, возвращает оператора
        public Operator moveTo() {

            if (operators[0].checkEmpty() && !operators[1].checkEmpty()) return operators[0];
            if (operators[1].checkEmpty() && !operators[0].checkEmpty()) return operators[1];
            if (!operators[0].checkEmpty() && !operators[1].checkEmpty())
                return (operators[0].QUEUE.size() <= operators[1].QUEUE.size()) ? operators[0] : operators[1];
            return operators[0];
        }


        //основная функция установки текущего глобального времени
        public void process() {

            if (flag) newTransactionTime = currentTime + generate(LEFTTRANS, RIGHTTRANS);//переменная хранит время прихода ближайшего транзакта

            float op1Time = operators[0].timeOut;//время конца обработки находящегося внутри первого оператора транзакта
            float op2Time = operators[1].timeOut;//время конца обработки находящегося внутри первого оператора транзакта
            //наименьшее из этих трёх времен становится текущим глобальным временем после их сравнения


            //если время конца обработки находящегося внутри первого оператора транзакта меньше остальных
            if (op1Time<=op2Time && op1Time<=newTransactionTime) {
                currentTime = op1Time;
                flag = false;//генерация нового транзакта не нужна, так он будет сгенирован ппри становлении его времени минимальным
                pw.printf("In %fs transaction ID#%d released the device number %d\n", op1Time, operators[0].getCurrentTransaction().getID(),1);
                pw.printf("In %fs transaction ID#%d released the model\n", op1Time, operators[0].getCurrentTransaction().getID());

                //если очереди нет, то есть нет больше транзактов на обработку, то мы помечаем оператор свободным
                if (operators[0].getQUEUE().size()==0) {
                    operators[0].setTimeOut(10000000000f);//оператор сейчас не участвует в сравнении времени
                    operators[0].setCurrentTransaction(null);//оператор сейчас не обрабатывает транзакты
                }
                else {
                    operators[0].setCurrentTransaction(null);//оператор сейчас не обрабатывает транзакты
                    Transaction trans = operators[0].QUEUE.getTrans(); //оператор готов взять новый транзакт на обработку из очереди
                    operators[0].operatorProcess(trans,2);//маркер 2 говорит о том, что в оператора идет транзакт, раннее вошедший в модель и стоящий в очереди
                    operators[0].getQUEUE().removeTrans();

                }
            }

            if (op2Time<=op1Time && op2Time<=newTransactionTime) {
                flag = false;
                currentTime = op2Time;
                pw.printf("In %fs transaction ID#%d released the device number %d\n", op2Time, operators[1].getCurrentTransaction().getID(),2);
                pw.printf("In %fs transaction ID#%d released the model\n", op2Time, operators[1].getCurrentTransaction().getID());

                if (operators[1].getQUEUE().size()==0) {
                    operators[1].setTimeOut(10000000000f);
                    operators[1].setCurrentTransaction(null);
                }
                else {
                    operators[1].setCurrentTransaction(null);
                    Transaction trans = operators[1].QUEUE.getTrans();
                    operators[1].operatorProcess(trans,2);
                    operators[1].getQUEUE().removeTrans();
                }
            }


            if (newTransactionTime<=op2Time && newTransactionTime<=op1Time) {
                flag = true;
                currentTime = newTransactionTime;
                transaction = new Transaction(newTransactionTime, ++transactionCount);//генерация транзакта
                pw.printf("In %fs transaction ID#%d entered the model\n", currentTime, transaction.getID());
                moveTo().operatorProcess(transaction,1);//маркер 1 говорит о том, что в оператора идёт только что пришедший(сгенериргованный транзакт)
            }
        }
    }


    //функция генерации рандомного действительного числа в промежутке, заданном границами
    public static float generate(float leftBoard, float rightBoard) {
        Random random = new Random();
        return leftBoard + random.nextFloat() * (rightBoard - leftBoard);
    }
}

