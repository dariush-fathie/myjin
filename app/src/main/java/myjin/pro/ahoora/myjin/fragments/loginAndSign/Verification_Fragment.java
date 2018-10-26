package myjin.pro.ahoora.myjin.fragments.loginAndSign;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import myjin.pro.ahoora.myjin.R;
import myjin.pro.ahoora.myjin.activitys.Login2Activity;
import myjin.pro.ahoora.myjin.models.TempModel;
import myjin.pro.ahoora.myjin.utils.ApiInterface;
import myjin.pro.ahoora.myjin.utils.KotlinApiClient;
import myjin.pro.ahoora.myjin.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Verification_Fragment extends Fragment implements OnClickListener {

    @SuppressLint("StaticFieldLeak")
    private static View view;
    @SuppressLint("StaticFieldLeak")
    private static AppCompatEditText etVeriftybox;
    @SuppressLint("StaticFieldLeak")
    private static AppCompatTextView tv_edit_phone_number, tv_signUp, tv_phone_number;
    protected IActivityEnabledListener aeListener;
    private static FragmentManager fragmentManager;
    String vc = "", number = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();

        if (args != null) {
            vc = args.getString("vc");
            number = args.getString("number");
        }

        view = inflater.inflate(R.layout.verification_layout, container, false);
        initViews();
        setListeners();

        return view;
    }


    protected interface IActivityEnabledListener {
        void onActivityEnabled(FragmentActivity activity);
    }

    protected void getAvailableActivity(IActivityEnabledListener listener) {
        if (getActivity() == null) {
            aeListener = listener;

        } else {
            listener.onActivityEnabled(getActivity());
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (aeListener != null) {
            aeListener.onActivityEnabled((FragmentActivity) context);
            aeListener = null;
        }
    }

    private void initViews() {

        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(FragmentActivity activity) {
                fragmentManager = activity.getSupportFragmentManager();
                etVeriftybox = view.findViewById(R.id.et_veriftybox);
                tv_edit_phone_number = view.findViewById(R.id.tv_edit_phone_number);
                tv_signUp = view.findViewById(R.id.tv_signUp);
                tv_phone_number = view.findViewById(R.id.tv_phone_number);
            }
        });

    }

    private void setListeners() {
        tv_signUp.setOnClickListener(this);
        tv_edit_phone_number.setOnClickListener(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setViewVal();
    }

    @SuppressLint("SetTextI18n")
    private void setViewVal() {
        tv_phone_number.setText("+98" + number.substring(1));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_signUp:
                checkValidation();
                break;

            case R.id.tv_edit_phone_number:

                new Login2Activity().replaceLoginFragment();
                break;
        }
    }

    private void checkValidation() {
        getAvailableActivity(new IActivityEnabledListener() {
            @Override
            public void onActivityEnabled(FragmentActivity activity) {
                if (etVeriftybox.getText() != null) {

                    if (etVeriftybox.getText().toString().trim().equals(vc)) {
                        signIn(number,activity);
                    }

                }
            }
        });


    }

    private void signIn(final String number, final FragmentActivity activity) {
        ApiInterface apiInterface = KotlinApiClient.INSTANCE.getClient().create(ApiInterface.class);

        apiInterface.signIn(number, "f", "l", "pr").enqueue(new Callback<TempModel>() {
            @Override
            public void onResponse(@NonNull Call<TempModel> call, @NonNull Response<TempModel> response) {
                if (response.isSuccessful()) {
                    String val ;
                    TempModel tempModel = response.body();
                    assert tempModel != null;
                    val = tempModel.getVal();

                    switch (val) {
                        case "I": {
                            Bundle bundle = new Bundle();
                            bundle.putString("number", number);

                            Fragment ynf = new YourName_Fragment();
                            ynf.setArguments(bundle);

                            fragmentManager
                                    .beginTransaction()
                                    .setCustomAnimations(R.anim.right_enter, R.anim.left_out)
                                    .replace(R.id.frameContainer, ynf,
                                            Utils.INSTANCE.getYourName_Fragment()).commit();
                            break;
                        }
                        case "U": {
                            Toast.makeText(activity, "go to profile", Toast.LENGTH_SHORT).show();
                            // TODO: 10/26/2018 goto profile
                            break;
                        }
                        case "no": {
                            Toast.makeText(activity, R.string.vbkhmsh, Toast.LENGTH_SHORT).show();
                            break;
                        }
                        case "empty": {
                            Toast.makeText(activity, R.string.ltmkhshesh, Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }

                }
            }

            @Override
            public void onFailure(@NonNull Call<TempModel> call, @NonNull Throwable t) {

            }
        });
    }


}


