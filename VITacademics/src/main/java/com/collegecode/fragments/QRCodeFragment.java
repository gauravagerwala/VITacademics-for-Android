package com.collegecode.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.collegecode.VITacademics.Home;
import com.collegecode.VITacademics.R;
import com.collegecode.objects.DataHandler;
import com.collegecode.objects.QRCreator.Contents;
import com.collegecode.objects.QRCreator.QRCodeEncoder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

/**
 * Created by saurabh on 5/11/14.
 */
public class QRCodeFragment extends Fragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_qr_code,container, false);
        ImageView imageView = (ImageView) v.findViewById(R.id.qrCode);
        String code = ((Home) getActivity()).token;
        int qrCodeDimention = 500;

        ((TextView) v.findViewById(R.id.lbl_token)).setText("PIN: " + code);
        ((TextView) v.findViewById(R.id.lbl_time_remaining)).setText(new DataHandler(getActivity()).getTokenExpiryTimeString());

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(code, null,
                Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimention);
        try {
            Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
            imageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return v;
    }
}
