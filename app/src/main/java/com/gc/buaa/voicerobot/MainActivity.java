package com.gc.buaa.voicerobot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ListView lvList;

    private ArrayList<ChatBean> mChatList = new ArrayList<ChatBean>();

    private ChatAdapter mAdapter;

    StringBuffer mTextBuffer = new StringBuffer();

    private String[] mMMAnswers = new String[] { "约吗?", "讨厌!", "不要再要了!", "这是最后一张了!", "漂亮吧?" };
    private int[] mMMImageIDs = new int[] { R.drawable.p1, R.drawable.p2, R.drawable.p3, R.drawable.p4 };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化语音引擎
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=57173200");

        lvList = (ListView) findViewById(R.id.lv_list);
        mAdapter = new ChatAdapter();

        lvList.setAdapter(mAdapter);
    }

    /**
     * 点击开始语音识别
     * @param view
     */
    public void startListen(View view){
        //1.创建RecognizerDialog对象
        RecognizerDialog mDialog = new RecognizerDialog(this, null);

        Log.i("test", "mDialog" + mDialog.toString());

        //2.设置accent、language等参数
        mDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        mDialog.setParameter(SpeechConstant.ACCENT, "mandarin");
        //若要将UI控件用于语义理解，必须添加以下参数设置，设置之后onResult回调返回将是语义理解
        //结果
//        mDialog.setParameter("asr_sch", "1");
//        mDialog.setParameter("nlp_version", "2.0");
        //3.设置回调接口
        mDialog.setListener(mRecognizerDialogListener);
        //4.显示dialog，接收语音输入
        mDialog.show();
    }

    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener(){

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean isLast) {
            Log.d("test", "recognizerResult:" + recognizerResult.getResultString());

            String text = parseData(recognizerResult.getResultString());
            //把多次解析的json连接起来
            mTextBuffer.append(text);

            if (isLast) {// 会话结束
                String finalText = mTextBuffer.toString();
                mTextBuffer = new StringBuffer();// 清理buffer
                Log.d("test", "最终结果:" + finalText);
                mChatList.add(new ChatBean(finalText, true, -1));

                String answer = "没听清楚";
                int imageId = -1;
                if (finalText.contains("你好")) {
                    answer = "大家好,才是真的好!";
                } else if (finalText.contains("你是谁")) {
                    answer = "我是你的小助手!";
                } else if (finalText.contains("诗")) {
                    answer = "白日依山尽，黄河入海流；欲穷千里目，更上一层楼。";
                } else if (finalText.contains("什么")||finalText.contains("多少")) {
                    answer = "你自己百度吧!";
                } else if (finalText.contains("歌")) {
                    answer = "一闪一闪亮晶晶，对不起，我唱的没有节奏了!";
                } else if (finalText.contains("天王盖地虎")) {
                    answer = "小鸡炖蘑菇";
                    imageId = R.drawable.m;
                } else if (finalText.contains("美女")) {
                    Random random = new Random();
                    int i = random.nextInt(mMMAnswers.length);
                    int j = random.nextInt(mMMImageIDs.length);
                    answer = mMMAnswers[i];
                    imageId = mMMImageIDs[j];
                }

                mChatList.add(new ChatBean(answer, false, imageId));// 添加回答数据
                mAdapter.notifyDataSetChanged();// 刷新listview

                lvList.setSelection(mChatList.size() - 1);// 定位到最后一个条目

                read(answer);
            }
        }

        @Override
        public void onError(SpeechError speechError) {
            Log.d("test", "speechError:" + speechError.getPlainDescription(true));
        }
    };

    class ChatAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mChatList.size();
        }

        @Override
        public ChatBean getItem(int position) {
            return mChatList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(MainActivity.this, R.layout.list_item, null);

                holder.tvAsk = (TextView) convertView.findViewById(R.id.tv_ask);
                holder.tvAnswer = (TextView) convertView.findViewById(R.id.tv_answer);
                holder.llAnswer = (LinearLayout) convertView.findViewById(R.id.ll_answer);
                holder.ivPic = (ImageView) convertView.findViewById(R.id.iv_pic);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ChatBean item = getItem(position);

            if (item.isAsker) {// 是提问者
                holder.tvAsk.setVisibility(View.VISIBLE);
                holder.llAnswer.setVisibility(View.GONE);

                holder.tvAsk.setText(item.text);
            } else {
                holder.tvAsk.setVisibility(View.GONE);
                holder.llAnswer.setVisibility(View.VISIBLE);
                holder.tvAnswer.setText(item.text);
                if (item.imageId != -1) {// 有图片
                    holder.ivPic.setVisibility(View.VISIBLE);
                    holder.ivPic.setImageResource(item.imageId);
                } else {
                    holder.ivPic.setVisibility(View.GONE);
                }
            }

            return convertView;
        }

    }

    static class ViewHolder {
        public TextView tvAsk;
        public TextView tvAnswer;
        public LinearLayout llAnswer;
        public ImageView ivPic;
    }

    /**
     * 解析语音数据
     *
     * @param resultString
     */
    protected String parseData(String resultString) {
        Gson gson = new Gson();
        VoiceBean bean = gson.fromJson(resultString, VoiceBean.class);
        ArrayList<VoiceBean.WSBean> ws = bean.ws;

        StringBuffer sb = new StringBuffer();
        for (VoiceBean.WSBean wsBean : ws) {
            String text = wsBean.cw.get(0).w;
            sb.append(text);
        }

        return sb.toString();
    }

    /**
     * 语音朗诵
     */
    public void read(String text) {
        SpeechSynthesizer mTts = SpeechSynthesizer.createSynthesizer(this, null);

        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        mTts.setParameter(SpeechConstant.SPEED, "50");
        mTts.setParameter(SpeechConstant.VOLUME, "80");
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);

        mTts.startSpeaking(text, null);
    }
}
