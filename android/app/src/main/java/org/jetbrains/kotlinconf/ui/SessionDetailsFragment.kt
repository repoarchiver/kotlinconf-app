package org.jetbrains.kotlinconf.ui

import android.graphics.*
import android.graphics.drawable.*
import android.os.*
import android.support.design.widget.*
import android.support.design.widget.AppBarLayout.LayoutParams.*
import android.support.design.widget.CollapsingToolbarLayout.LayoutParams.*
import android.support.v4.app.*
import android.support.v7.app.*
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.*
import com.bumptech.glide.*
import kotlinx.coroutines.*
import kotlinx.coroutines.android.*
import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout
import org.jetbrains.anko.appcompat.v7.toolbar
import org.jetbrains.anko.applyRecursively
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.design.*
import org.jetbrains.anko.dimen
import org.jetbrains.anko.dip
import org.jetbrains.anko.imageButton
import org.jetbrains.anko.imageResource
import org.jetbrains.anko.imageView
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.support.v4.*
import org.jetbrains.anko.textColor
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.view
import org.jetbrains.anko.wrapContent
import org.jetbrains.kotlinconf.*
import org.jetbrains.kotlinconf.R
import org.jetbrains.kotlinconf.data.*
import org.jetbrains.kotlinconf.presentation.SessionDetailsPresenter
import org.jetbrains.kotlinconf.presentation.SessionDetailsView

class SessionDetailsFragment : Fragment(), SessionDetailsView {

    private lateinit var toolbar: Toolbar
    private lateinit var speakersTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var detailsTextView: TextView
    private lateinit var descriptionTextView: TextView
    private val speakerImageViews: MutableList<ImageView> = mutableListOf()
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var favoriteButton: FloatingActionButton

    private lateinit var goodButton: ImageButton
    private lateinit var badButton: ImageButton
    private lateinit var okButton: ImageButton

    private val sessionId by lazy { arguments!!.get(KEY_SESSION_ID) as String }
    private val repository by lazy { (activity!!.application as KotlinConfApplication).dataRepository }
    private val presenter by lazy { SessionDetailsPresenter(UI, this, sessionId, repository) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setUpActionBar()

        favoriteButton.setOnClickListener { presenter.onFavoriteButtonClicked() }
        goodButton.setOnClickListener { presenter.rateSessionClicked(SessionRating.GOOD) }
        okButton.setOnClickListener { presenter.rateSessionClicked(SessionRating.OK) }
        badButton.setOnClickListener { presenter.rateSessionClicked(SessionRating.BAD) }
        presenter.onCreate()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    override fun setIsFavorite(isFavorite: Boolean) {
        val favoriteIcon = if (isFavorite) R.drawable.ic_favorite_white_24dp else R.drawable.ic_favorite_border_white_24dp
        favoriteButton.setImageResource(favoriteIcon)
    }

    override fun setupRatingButtons(rating: SessionRating?) {
        fun selectButton(target: SessionRating): Int = when (rating) {
            target -> R.drawable.round_toggle_button_background_selected
            else -> R.drawable.round_toggle_button_background
        }

        goodButton.backgroundResource = selectButton(SessionRating.GOOD)
        okButton.backgroundResource = selectButton(SessionRating.OK)
        badButton.backgroundResource = selectButton(SessionRating.BAD)
    }

    override fun setRatingClickable(clickable: Boolean) {
        goodButton.isClickable = clickable
        okButton.isClickable = clickable
        badButton.isClickable = clickable
    }

    override fun updateView(session: SessionModel) {
        with(session) {
            collapsingToolbar.title = session.title
            speakersTextView.text = session.speakers.joinToString(separator = ", ") { it.fullName }
            val time = (session.startsAt to session.endsAt).toReadableString()
            timeTextView.text = time
            detailsTextView.text = listOfNotNull(roomText, category).joinToString(", ")
            descriptionTextView.text = session.descriptionText

            session.speakers
                    .takeIf { it.size < 3 }
                    ?.map { it.profilePicture }
                    ?.apply {
                        forEachIndexed { index, imageUrl ->
                            imageUrl?.let { speakerImageViews[index].showSpeakerImage(it) }
                        }
                    }
        }
    }

    private val SessionModel.roomText: String?
        get() = room?.let { getString(R.string.room_format_details, it) }

    private fun setUpActionBar() {
        setHasOptionsMenu(true)
        (activity as AppCompatActivity).apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun ImageView.showSpeakerImage(imageUrl: String) {
        visibility = View.VISIBLE
        Glide.with(this@SessionDetailsFragment)
                .load(imageUrl)
                .centerCrop()
                .into(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return UI {
            coordinatorLayout {
                lparams(width = matchParent, height = matchParent)
                themedAppBarLayout(R.style.ThemeOverlay_AppCompat_Dark_ActionBar) {
                    id = R.id.app_bar_layout
                    collapsingToolbar = multilineCollapsingToolbarLayout {
                        contentScrim = ColorDrawable(theme.getColor(R.attr.colorPrimary))
                        maxLines = 5
                        expandedTitleMarginStart = dip(20)
                        expandedTitleMarginEnd = dip(20)
                        setExpandedTitleTextAppearance(R.style.SessionTitleExpanded)

                        linearLayout {
                            layoutParams = CollapsingToolbarLayout.LayoutParams(matchParent, matchParent).apply {
                                collapseMode = COLLAPSE_MODE_PARALLAX
                            }

                            imageView {
                                visibility = View.GONE
                            }.lparams(width = 0, height = matchParent) {
                                weight = 0.5f
                            }.also { speakerImageViews.add(it) }

                            imageView {
                                visibility = View.GONE
                            }.lparams(width = 0, height = matchParent) {
                                weight = 0.5f
                            }.also { speakerImageViews.add(it) }
                        }

                        view {
                            backgroundResource = R.drawable.appbar_buttons_scrim
                            layoutParams = CollapsingToolbarLayout.LayoutParams(
                                    matchParent,
                                    dimen(context.getResourceId(R.attr.actionBarSize))
                            ).apply {
                                gravity = Gravity.TOP
                            }
                        }

                        view {
                            backgroundResource = R.drawable.appbar_title_scrim
                            layoutParams = CollapsingToolbarLayout.LayoutParams(matchParent, matchParent).apply {
                                gravity = Gravity.BOTTOM
                            }
                        }

                        toolbar = toolbar {
                            layoutParams = CollapsingToolbarLayout.LayoutParams(
                                    matchParent,
                                    dimen(context.getResourceId(R.attr.actionBarSize))
                            ).apply {
                                collapseMode = COLLAPSE_MODE_PIN
                            }
                        }
                    }.lparams(width = matchParent, height = matchParent) {
                        scrollFlags = SCROLL_FLAG_SCROLL or SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
                    }
                }.lparams(width = matchParent, height = dip(256))

                favoriteButton = floatingActionButton().lparams {
                    anchorId = R.id.app_bar_layout
                    anchorGravity = Gravity.BOTTOM or Gravity.END
                    margin = dip(8)
                }

                nestedScrollView {
                    verticalLayout {
                        speakersTextView = textView {
                            textSize = 26f
                            textColor = Color.BLACK
                        }.lparams {
                            bottomMargin = dip(6)
                        }

                        timeTextView = textView {
                            textSize = 17f
                        }.lparams {
                            bottomMargin = dip(4)
                        }

                        detailsTextView = textView {
                            textSize = 17f
                        }

                        descriptionTextView = textView {
                            textSize = 19f
                        }.lparams {
                            topMargin = dip(20)
                        }

                        linearLayout {
                            goodButton = imageButton {
                                imageResource = R.drawable.ic_thumb_up_white_24dp
                            }
                            okButton = imageButton {
                                imageResource = R.drawable.ic_sentiment_neutral_white_36dp
                            }
                            badButton = imageButton {
                                imageResource = R.drawable.ic_thumb_down_white_24dp
                            }
                        }.lparams {
                            topMargin = dip(10)
                            bottomMargin = dip(80)
                            gravity = Gravity.CENTER_HORIZONTAL
                        }.applyRecursively { view ->
                            when (view) {
                                is ImageButton -> {
                                    view.lparams {
                                        width = dip(56)
                                        height = dip(56)
                                        gravity = Gravity.CENTER_VERTICAL
                                        margin = dip(10)
                                    }
                                }
                            }
                        }

                    }.lparams(width = matchParent, height = wrapContent) {
                        margin = dip(20)
                    }.applyRecursively { view ->
                        (view as? TextView)?.setTextIsSelectable(true)
                    }

                }.lparams(width = matchParent, height = matchParent) {
                    behavior = AppBarLayout.ScrollingViewBehavior()
                }
            }
        }.view
    }

    companion object {
        const val TAG = "SessionDetailsFragment"
        private const val KEY_SESSION_ID = "SessionId"

        fun forSession(id: String): SessionDetailsFragment = SessionDetailsFragment().apply {
            arguments = Bundle().apply { putString(KEY_SESSION_ID, id) }
        }
    }
}