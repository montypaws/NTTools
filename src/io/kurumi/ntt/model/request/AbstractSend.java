package io.kurumi.ntt.model.request;

import com.pengrad.telegrambot.response.*;
import io.kurumi.ntt.fragment.*;
import io.kurumi.ntt.model.*;
import io.kurumi.ntt.utils.*;

public abstract class AbstractSend<T extends AbstractSend> {

	public Msg origin;

    public Fragment fragment;

    public AbstractSend(Fragment fragment) {
        this.fragment = fragment;
    }

    public abstract T buttons(ButtonMarkup markup);

    public abstract T enableLinkPreview();

    public abstract T markdown();

    public abstract T html();

    public abstract BaseResponse sync();

    public abstract BaseResponse sync(Exception track);

	
    public void failed() {
        
        failed(5000);
        
    }
    
    public void failed(final long delay) {

        if (origin == null) return;

        final Exception track = new Exception();

        ThreadPool.exec(new Runnable() {

                @Override
                public void run() {

                    BaseResponse resp = sync(track);

                    if (resp.isOk()) {

                        io.kurumi.ntt.utils.T.tryDelete(delay,origin);

                    }


                }

            });
            
            
      }
    

    public void exec() {

        final Exception track = new Exception();

        ThreadPool.exec(new Runnable() {

                @Override
                public void run() {

                    sync(track);

                }

            });

    }





}
