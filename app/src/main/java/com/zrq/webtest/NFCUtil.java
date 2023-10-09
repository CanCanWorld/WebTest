package com.zrq.webtest;

import android.content.Context;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.util.Log;
import android.widget.Toast;


import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;


public class NFCUtil {

    private static final String ENCRYPTKEY = "topscomm";

    /**
     * 读数据
     * @param context
     * @param mTag
     * @return
     */
    public static String readMessage(Context context, Tag mTag) {
        String msg = "";
        if (mTag==null){
            Toast.makeText(context, "不能识别的标签类型!", Toast.LENGTH_SHORT).show();
            return msg;
        }
        Ndef ndef=Ndef.get(mTag);//获取ndef对象
        if(ndef == null){
            return msg;
        }
        try {
            ndef.connect();//连接
            NdefMessage ndefMessage=ndef.getNdefMessage();//获取NdefMessage对象
            if (ndefMessage!=null){
                msg = parseTextRecord(ndefMessage.getRecords()[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                ndef.close();//关闭链接
            } catch (IOException e) {
                e.printStackTrace();
            }
            // DES解密
            if(!Objects.equals(msg, "")){
                msg = SecurityUtil.decryptDES(msg, ENCRYPTKEY);
            }
            return msg;
        }
    }

    /**
     * 解析NDEF文本数据，从第三个字节开始，后面的文本数据
     * @param ndefRecord
     * @return
     */
    public static String parseTextRecord(NdefRecord ndefRecord) {
        /**
         * 判断数据是否为NDEF格式
         */
        //判断TNF
        if (ndefRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
            return null;
        }
        //判断可变的长度的类型
        if (!Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
            return null;
        }
        try {
            //获得字节数组，然后进行分析
            byte[] payload = ndefRecord.getPayload();
            //下面开始NDEF文本数据第一个字节，状态字节
            //判断文本是基于UTF-8还是UTF-16的，取第一个字节"位与"上16进制的80，16进制的80也就是最高位是1，
            //其他位都是0，所以进行"位与"运算后就会保留最高位
            String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
            //3f最高两位是0，第六位是1，所以进行"位与"运算后获得第六位
            int languageCodeLength = payload[0] & 0x3f;
            //下面开始NDEF文本数据第二个字节，语言编码
            //获得语言编码
            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            //下面开始NDEF文本数据后面的字节，解析出文本
            String textRecord = new String(payload, languageCodeLength + 1,
                    payload.length - languageCodeLength - 1, textEncoding);
            return textRecord;
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * 写数据
     * @param context
     * @param mTag
     * @param message
     */
    public static void writeMessage(Context context, Tag mTag, String message) {
        if (mTag==null){
            Toast.makeText(context, "不能识别的标签类型!", Toast.LENGTH_SHORT).show();
            return;
        }
        Ndef ndef=Ndef.get(mTag);//获取ndef对象
        if (ndef == null || !ndef.isWritable()){
            Toast.makeText(context, "该标签不能写入数据!", Toast.LENGTH_SHORT).show();
            return;
        }
        // DES加密
        message = SecurityUtil.encryptDES(message, ENCRYPTKEY);
        NdefRecord ndefRecord=createTextRecord(message);//创建一个NdefRecord对象
        NdefMessage ndefMessage=new NdefMessage(new NdefRecord[]{ndefRecord});//根据NdefRecord数组，创建一个NdefMessage对象
        int size=ndefMessage.getByteArrayLength();
        if (ndef.getMaxSize()<size){
            Toast.makeText(context, "标签容量不足!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            ndef.connect();//连接
            ndef.writeNdefMessage(ndefMessage);//写数据
            Toast.makeText(context, "数据写入成功!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }finally {
            try {
                ndef.close();//关闭连接
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 创建NDEF文本数据
     * @param text
     * @return
     */
    public static NdefRecord createTextRecord(String text) {
        byte[] langBytes = Locale.CHINA.getLanguage().getBytes(Charset.forName("US-ASCII"));
        Charset utfEncoding = Charset.forName("UTF-8");
        //将文本转换为UTF-8格式
        byte[] textBytes = text.getBytes(utfEncoding);
        //设置状态字节编码最高位数为0
        int utfBit = 0;
        //定义状态字节
        char status = (char) (utfBit + langBytes.length);
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        //设置第一个状态字节，先将状态码转换成字节
        data[0] = (byte) status;
        //设置语言编码，使用数组拷贝方法，从0开始拷贝到data中，拷贝到data的1到langBytes.length的位置
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        //设置文本字节，使用数组拷贝方法，从0开始拷贝到data中，拷贝到data的1 + langBytes.length
        //到textBytes.length的位置
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
        //通过字节传入NdefRecord对象
        //NdefRecord.RTD_TEXT：传入类型 读写
        NdefRecord ndefRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT, new byte[0], data);
        return ndefRecord;
    }

}
