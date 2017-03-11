package br.com.vegetalia.app.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import br.com.vegetalia.app.R;

public class DialogUtils {

    public interface OnDismiss {
        void onDismiss();
    }

    public static void alert(final Activity activity, String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message)
                .setPositiveButton(R.string.dialog_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                //activity.startActivity(new Intent(action));
                                d.dismiss();
                            }
                        });
        builder.create().show();
    }

    public static void alertCustomLayout(final Activity activity, int customLayout, final OnDismiss callback) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(customLayout)
                .setPositiveButton(R.string.dialog_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                d.dismiss();
                                if (callback != null) callback.onDismiss();
                            }
                        });
        builder.create().show();
    }

    public interface OnConfirm {
        void onConfirm();
    }


    public static void confirm(Activity activity, String message, final OnConfirm callback) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message)
                .setNegativeButton(R.string.dialog_no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                d.dismiss();
                            }
                        })
                .setPositiveButton(R.string.dialog_yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                d.dismiss();
                                if (callback != null) callback.onConfirm();
                            }
                        });
        builder.create().show();
    }
}
