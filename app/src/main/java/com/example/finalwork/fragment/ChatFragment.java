package com.example.finalwork.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.finalwork.R;
import com.tencent.qcloud.tuicore.TUILogin;
import com.tencent.qcloud.tuicore.interfaces.TUICallback;
import com.tencent.qcloud.tuikit.tuiconversation.minimalistui.page.TUIConversationMinimalistFragment;

/**
 * 使用腾讯云 TUIKit 会话列表（Minimalist UI）
 */
public class ChatFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_tui_conversation_container, container, false);

        SharedPreferences sp = requireActivity().getSharedPreferences("user_info", 0);
        String userId = sp.getString("login_user_id", "");
        boolean isLogin = sp.getBoolean("is_login", false);
        if (TextUtils.isEmpty(userId) || !isLogin) {
            Toast.makeText(getActivity(), "请先登录", Toast.LENGTH_SHORT).show();
            return root;
        }

        // 已在 Login/Main 登录 TUILogin，这里仅挂载会话列表 Fragment
        attachConversationFragment();
        return root;
    }

    //构建会话列表界面
    private void attachConversationFragment() {
        Fragment fragment = getChildFragmentManager().findFragmentByTag("tui_conversation");
        if (fragment == null) {
            fragment = new TUIConversationMinimalistFragment();
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.tui_conversation_container, fragment, "tui_conversation")
                    .commitAllowingStateLoss();
        }
    }
}