package com.amaze.filemanager.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class CircularColorsView extends View {

    private static final float DISTANCE_PERCENTUAL = 0.08f; // %khoảng cách
    private static final float DIAMETER_PERCENTUAL = 0.65f; // %đường kính
    private static final int SEMICIRCLE_LINE_WIDTH = 0; //

    private boolean paintInitialized = false;   // khởi tạo màu = false
    private Paint dividerPaint = new Paint();   // dải phân cách
    private Paint[] colors = {new Paint(), new Paint(), new Paint(), new Paint()};
    private RectF semicicleRect = new RectF();

    public CircularColorsView(Context context) {
        super(context);
        init();
    }


    public CircularColorsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dividerPaint.setColor(Color.BLACK); // màu đen
        dividerPaint.setStyle(Paint.Style.STROKE); // kiểu sơn nét chữ
        dividerPaint.setFlags(Paint.ANTI_ALIAS_FLAG); // khử răng cưa khi vẽ
        dividerPaint.setStrokeWidth(SEMICIRCLE_LINE_WIDTH); //Đặt width cho stroke
    }

    public void setDividerColor(int color) {
        dividerPaint.setColor(color);
    } // thiết lập bố cục màu

    public void setColors(int color, int color1, int color2, int color3) {
        colors[0].setColor(color);
        colors[1].setColor(color1);
        colors[2].setColor(color2);
        colors[3].setColor(color3);

        for (Paint p : colors) p.setFlags(Paint.ANTI_ALIAS_FLAG);

        paintInitialized = true; // bắt đầu khởi tạo màu
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(isInEditMode()) setColors(Color.CYAN, Color.RED, Color.GREEN, Color.BLUE); // đang ở chế độ chỉnh sửa
        if(!paintInitialized) throw new IllegalStateException("Paint has not actual color!"); // ném ra lỗi nếu ko sơn được màu

        float distance = getWidth() * DISTANCE_PERCENTUAL; // khoảng cách

        float diameterByHeight = getHeight()* DIAMETER_PERCENTUAL; // đường kính theo chiều cao
        float diameterByWidth = (getWidth() - distance*2)/3f* DIAMETER_PERCENTUAL; // đường kính theo chiều rộng
        float diameter = Math.min(diameterByHeight, diameterByWidth); //đường kính

        float radius = diameter/2f; // bán kính

        int centerY = getHeight()/2;
        float[] positionX = {getWidth()- diameter - distance - diameter - distance - radius,
                getWidth() - diameter - distance - radius,
                getWidth() - radius};
        semicicleRect.set(positionX[0]- radius, centerY- radius, positionX[0]+ radius, centerY+ radius);

        canvas.drawArc(semicicleRect, 90, 180, true, colors[0]);
        canvas.drawArc(semicicleRect, 270, 180, true, colors[1]);

        canvas.drawLine(semicicleRect.centerX(), semicicleRect.top, semicicleRect.centerX(),
                semicicleRect.bottom, dividerPaint);

        canvas.drawCircle(positionX[1], centerY, radius, colors[2]);
        canvas.drawCircle(positionX[2], centerY, radius, colors[3]);
    }

}
