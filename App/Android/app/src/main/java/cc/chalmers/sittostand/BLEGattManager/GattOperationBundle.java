package cc.chalmers.sittostand.BLEGattManager;

import java.util.ArrayList;

import com.example.android.bluetoothlegatt.BLEGattManager.operations.GattOperation;

public class GattOperationBundle {
    final ArrayList<GattOperation> operations;

    public GattOperationBundle() {
        operations = new ArrayList<>();
    }

    public void addOperation(GattOperation operation) {
        operations.add(operation);
        operation.setBundle(this);

    }

    public ArrayList<GattOperation> getOperations() {
        return operations;
    }
}
