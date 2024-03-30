import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.cmpe277_hackathon.R
import com.example.cmpe277_hackathon.annotationrecord.AnnotationRecord

@SuppressLint("SetTextI18n")
class AnnotationTableRowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var indicatorChoices: Map<String, String>? = null
    var countryChoices: Map<String, String>? = null

    var annotation = AnnotationRecord("?", "?", "?", "No Content")
        set(value) {
            field = value
            textView1.text = "${annotation.year} ${countryChoices?.get(annotation.country)}\n${indicatorChoices?.get(annotation.indicator)}"
            textView2.text = annotation.content
        }

    var onDelete: (() -> Unit)? = null

    private val textView1: TextView
    private val textView2: TextView
    private val deleteButton: Button

    init {
        orientation = HORIZONTAL
        setPadding(0, 0, 0, 24)
        gravity = Gravity.CENTER_VERTICAL

        deleteButton = Button(context).apply {
            text = "Delete"
            setOnClickListener {
                onDelete?.invoke()
            }
        }

        textView1 = TextView(context).apply {
            setTextAppearance(androidx.appcompat.R.style.TextAppearance_AppCompat_Caption)
            text = "${annotation.year} — ${annotation.country} — ${indicatorChoices?.get(annotation.indicator)}"
        }

        textView2 = TextView(context).apply {
            setTextAppearance(androidx.appcompat.R.style.TextAppearance_AppCompat_Body1)
            text = annotation.content
        }

        addView(deleteButton)
        addView(LinearLayout(context).apply {
            setPadding(16,0,0,0)
            orientation = VERTICAL
            addView(textView1)
            addView(textView2)
        })

    }
}
