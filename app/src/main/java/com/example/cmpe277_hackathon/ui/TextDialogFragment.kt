import android.app.AlertDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.widget.EditText
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import com.example.cmpe277_hackathon.R

class TextDialogFragment() : DialogFragment() {
    var textDialogListener: OnTextDialogListener? = null

    private var title: String? = null
    private var description: String? = null
    private var text: String? = null

    constructor(title: String?, description: String? = null, text: String? = null) : this() {
        this.title = title
        this.description = description
        this.text = text
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val editText = EditText(requireContext())
        editText.hint = "Enter your text here"
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        editText.layoutParams = layoutParams
        editText.textCursorDrawable?.setTint(requireContext().getColor(R.color.purple_500))
        editText.backgroundTintList = ColorStateList.valueOf(requireContext().getColor(R.color.purple_500))

        val builder = AlertDialog.Builder(requireContext())
        builder.apply {
            setTitle(title)
            setMessage(description)
            setView(editText)
            setPositiveButton("OK") { dialog, id ->
                val text = editText.text.toString()
                textDialogListener?.onTextDialogDataReceived(text)
            }
            setNegativeButton("Cancel", null)
        }

        return builder.create()
    }
}

interface OnTextDialogListener {
    fun onTextDialogDataReceived(textData: String)
}
