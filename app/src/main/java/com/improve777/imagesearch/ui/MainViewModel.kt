package com.improve777.imagesearch.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.rxjava2.cachedIn
import com.improve777.imagesearch.base.BaseViewModel
import com.improve777.imagesearch.domain.model.Image
import com.improve777.imagesearch.domain.repository.ImageRepository
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.SerialDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val imageRepository: ImageRepository,
) : BaseViewModel() {

    private val _isEmptyImages = MutableLiveData(true)
    val isEmptyImages: LiveData<Boolean> = _isEmptyImages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val serialDisposable = SerialDisposable()
    private val searchSubject = BehaviorSubject.create<String>()

    private val _images = MutableLiveData<PagingData<Image>>()
    val images: LiveData<PagingData<Image>> = _images

    init {
        subscribeSearchBar()
    }

    private fun subscribeSearchBar() {
        searchSubject
            .subscribeOn(Schedulers.computation())
            .doOnNext { fetchLoading(true) }
            .debounce(1000, TimeUnit.MILLISECONDS)
            .distinctUntilChanged()
            .onErrorResumeNext(Observable.just(""))
            .subscribeBy(onNext = this::searchImages)
            .addTo(compositeDisposable)
    }

    private fun searchImages(text: String) {
        imageRepository.getImages(text, 1, 30)
            .cachedIn(viewModelScope)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { fetchLoading(false) }
            .subscribeBy(onNext = _images::setValue)
            .also(serialDisposable::set)
            .addTo(compositeDisposable)
    }

    private fun fetchLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    fun onAfterTextChanged(text: String?) {
        searchSubject.onNext(text ?: "")
    }

    fun fetchEmpty(isEmpty: Boolean) {
        _isEmptyImages.value = isEmpty
    }
}