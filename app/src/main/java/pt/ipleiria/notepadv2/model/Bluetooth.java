package pt.ipleiria.notepadv2.model;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public abstract class Bluetooth implements AppConstants{

    static String TAG_SERVER = "Server_Task";
    static String TAG_SEND_DATA = "SendData_Task";

    public static BluetoothServerSocket bluetoothServerSocket;
    public static BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    public void setSocket(BluetoothServerSocket bluetoothServerSocket) {
        this.bluetoothServerSocket = bluetoothServerSocket;
    }

    public static ArrayList<String> getBoundedDevicesNames(){
        Set<BluetoothDevice> bluetoothDevices = Bluetooth.bluetoothAdapter.getBondedDevices();
        ArrayList<String> bluetoothDevicesName = new ArrayList<>();

        for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
            bluetoothDevicesName.add(bluetoothDevice.getName());
        }
        return bluetoothDevicesName;
    }

    public static BluetoothDevice getBluetoothDevice(String deviceName){
        Set<BluetoothDevice> bluetoothDevices = Bluetooth.bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
            if(bluetoothDevice.getName().equals(deviceName))
                return bluetoothDevice;
        }

        return null;
    }

    public static String getUuids(){
        return "00000000-0000-1000-8000-00805F9B34FB";
        //TODO return bluetoothAdapter.getAddress();
    }

    public static class Server extends AsyncTask<Object, Void, Object>{

        @Override
        protected void onPreExecute() {
            try {
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("AlzheimerNotes_BluetoothServer", UUID.fromString(getUuids()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Object doInBackground(Object... objects) {
            BluetoothSocket socket;
            Object data = null;

            while (bluetoothServerSocket != null) { // keep listening until exception occurs or a socket is returned
                try {
                    socket = bluetoothServerSocket.accept();
                    Log.i(TAG_SERVER, "Connection established.");
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                if (socket != null) { // if a connection was accepted
                    try {
                        data = receiveData(socket);

                        Log.i(TAG_SERVER, "Data received from "
                                + socket.getRemoteDevice().getName() + ": "
                                + data);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        bluetoothServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        break; // stop listening
                    }
                }
            }
            return data;
        }

        private Object receiveData(BluetoothSocket socket) throws IOException {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

            String encodedData = null;
            try {
                encodedData = (String) objectInputStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return decodeNotepadDataBluetooth(encodedData);

            /*
            InputStream inputStream = socket.getInputStream();

            java.util.Scanner s = new java.util.Scanner(inputStream).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
            */
        }
    }

    public static class SendData extends AsyncTask<Object, Void, String> {

        private BluetoothDevice bluetoothDevice;
        private BluetoothSocket bluetoothSocket;

        public SendData(BluetoothDevice bluetoothDevice) {
            this.bluetoothDevice = bluetoothDevice;

            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(getUuids()));
                Log.i(TAG_SEND_DATA, "Socket created.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(Object... objects) {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

            try {
                bluetoothSocket.connect();
                Log.i("ConnectTask", "Connected to remote device.");
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
                return "ERROR: " + e.getMessage();
            }
            try {
                sendData(bluetoothSocket, objects[0]);
                Log.i("ConnectTask", "Data sent: " + objects[0]);
            } catch (IOException e) {
                e.printStackTrace();
                return "ERROR: " + e.getMessage();
            }

            return "Data sent to " + bluetoothDevice.getName() + ": " + objects[0];
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                try {
                    bluetoothSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendData(BluetoothSocket socket, Object data) throws IOException {
            /*
            OutputStream outputStream = socket.getOutputStream();
            PrintStream printStream = new PrintStream(outputStream);
            printStream.print(data);
            printStream.close();
            */

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(encodeNotepadDataBluetooth((Notepad) data));
            objectOutputStream.close();
        }
    }

    private static String encodeNotepadDataBluetooth(Notepad data){
        String returnString = "";
        for(Note n : data.getNotes()){
            returnString+=n.getTitle();
            returnString+=NEW_FIELD_EXPORT_FILE;

            returnString+=n.getPlainKeywords();
            returnString+=NEW_FIELD_EXPORT_FILE;

            returnString+=n.getText();

            returnString+=NEW_NOTE_EXPORT_FILE;
        }

        return returnString;
    }

    private static Notepad decodeNotepadDataBluetooth(String encodedNotepad){
        if(encodedNotepad.equals("")){
            return null;
        }

        Notepad notepad = new Notepad();

        String[] notesAsString = encodedNotepad.split(NEW_NOTE_EXPORT_FILE);
        for(String noteAsString : notesAsString){
            String[] noteFields = noteAsString.split(NEW_FIELD_EXPORT_FILE);
            Note n = new Note(noteFields[0], noteFields[1]);
            n.setText(noteFields[2]);
            notepad.addNote(n);
        }

        return notepad;
    }

}
